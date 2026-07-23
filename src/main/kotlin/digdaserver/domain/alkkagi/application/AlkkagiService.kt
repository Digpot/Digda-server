package digdaserver.domain.alkkagi.application

import digdaserver.domain.alkkagi.domain.AlkkagiGame
import digdaserver.domain.alkkagi.presentation.dto.AlkkagiEvent
import digdaserver.domain.alkkagi.presentation.dto.AlkkagiGameResponse
import digdaserver.domain.alkkagi.presentation.dto.AlkkagiMoveRequest
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

/**
 * 알까기 대전 오케스트레이션 — 초대(REST) / 진행(STOMP) / 만료(스케줄러).
 * 오목([digdaserver.domain.omok.application.OmokService])과 동일한 플로우:
 *
 *  1. 초대자가 같은 그룹 멤버 1명에게 초대(돌 개수 1~10 설정) → WAITING 대국 생성,
 *     상대에게 GAME_INVITE 알림(relatedType=ALKKAGI) 발송.
 *  2. 상대 수락 → ACTIVE + ACCEPTED 브로드캐스트. 거절/취소도 각각 브로드캐스트.
 *  3. 수(플릭 결과)는 STOMP 로 받아 반영하고 MOVE/FINISHED 브로드캐스트.
 *     물리 시뮬은 클라이언트가 수행 — 서버는 턴 검증·승패 판정·중계만 한다.
 *  4. 대기 10분 / 진행 방치 60분 초과 대국은 스케줄러가 EXPIRED 처리.
 */
@Service
class AlkkagiService(
    private val gameManager: AlkkagiGameManager,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ── 초대 (REST) ─────────────────────────────────────────────

    // 초대 알림이 notification 테이블에 INSERT 하므로 readOnly 금지.
    @Transactional
    fun createGame(
        inviterId: UUID,
        groupRoomId: Long,
        inviteeId: UUID,
        stoneCount: Int,
        formation: AlkkagiGame.Formation
    ): AlkkagiGameResponse {
        if (inviterId == inviteeId) throw DigdaException(ErrorCode.MINIGAME_SELF_INVITE)
        validateGroupMember(groupRoomId, inviterId)
        validateGroupMember(groupRoomId, inviteeId)

        val inviter = userRepository.findById(inviterId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }
        val invitee = userRepository.findById(inviteeId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val game = gameManager.create(
            groupRoomId = groupRoomId,
            inviterId = inviterId,
            inviterName = inviter.displayedName(),
            inviteeId = inviteeId,
            inviteeName = invitee.displayedName(),
            stoneCount = stoneCount,
            inviterFormation = formation
        )
        notificationService.notifyMinigameInvite(
            groupRoomId = groupRoomId,
            inviterUserId = inviterId,
            inviteeUserIds = listOf(inviteeId),
            gameId = game.id,
            gameType = "ALKKAGI",
            gameDisplayName = "알까기"
        )
        return AlkkagiGameResponse.from(game)
    }

    fun getGame(userId: UUID, gameId: Long): AlkkagiGameResponse {
        val game = gameManager.get(gameId)
        if (!game.isParticipant(userId)) {
            throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        }
        return AlkkagiGameResponse.from(game)
    }

    fun accept(
        userId: UUID,
        gameId: Long,
        formation: AlkkagiGame.Formation
    ): AlkkagiGameResponse {
        val game = gameManager.get(gameId)
        game.accept(userId, formation)
        log.info(
            "action=alkkagi_accept, gameId={}, userId={}, formation={}",
            gameId,
            userId,
            formation
        )
        return broadcast(game, AlkkagiEvent.Type.ACCEPTED)
    }

    fun decline(userId: UUID, gameId: Long): AlkkagiGameResponse {
        val game = gameManager.get(gameId)
        game.decline(userId)
        log.info("action=alkkagi_decline, gameId={}, userId={}", gameId, userId)
        return broadcast(game, AlkkagiEvent.Type.DECLINED)
    }

    fun cancel(userId: UUID, gameId: Long): AlkkagiGameResponse {
        val game = gameManager.get(gameId)
        game.cancel(userId)
        log.info("action=alkkagi_cancel, gameId={}, userId={}", gameId, userId)
        return broadcast(game, AlkkagiEvent.Type.CANCELED)
    }

    // ── 진행 (STOMP) ────────────────────────────────────────────

    fun move(userId: UUID, gameId: Long, request: AlkkagiMoveRequest) {
        val game = gameManager.get(gameId)
        val finished = game.applyMove(
            userId = userId,
            stoneId = request.stoneId,
            updates = request.stones.map {
                AlkkagiGame.StoneUpdate(id = it.id, x = it.x, y = it.y, alive = it.alive)
            }
        )
        log.info(
            "action=alkkagi_move, gameId={}, userId={}, stoneId={}, finished={}",
            gameId,
            userId,
            request.stoneId,
            finished
        )
        val type = if (finished) AlkkagiEvent.Type.FINISHED else AlkkagiEvent.Type.MOVE
        broadcast(
            game,
            type,
            flick = AlkkagiEvent.Flick(
                byUserId = userId,
                stoneId = request.stoneId,
                vx = request.flickVx,
                vy = request.flickVy
            )
        )
    }

    fun forfeit(userId: UUID, gameId: Long) {
        val game = gameManager.get(gameId)
        game.forfeit(userId)
        log.info("action=alkkagi_forfeit, gameId={}, userId={}", gameId, userId)
        broadcast(game, AlkkagiEvent.Type.FINISHED)
    }

    // ── 턴 제한시간 (30초 + 시뮬·통신 그레이스) ──────────────────

    /**
     * 30초 안에 튕기지 않은 턴을 상대에게 넘기고 TIMEOUT 을 브로드캐스트한다.
     * 그레이스 6초 — 마감 직전에 튕긴 수는 클라이언트 물리 시뮬(수 초)이 끝난 뒤
     * 도착하므로, 표시(30초)보다 넉넉히 늦게 판정해 정당한 수를 지키운다.
     */
    @Scheduled(fixedDelay = 3_000, initialDelay = 10_000)
    fun enforceTurnLimit() {
        gameManager.allActive().forEach { game ->
            if (game.timeoutTurnIfExpired(TURN_LIMIT_WITH_GRACE)) {
                log.info("action=alkkagi_turn_timeout, gameId={}", game.id)
                broadcast(game, AlkkagiEvent.Type.TIMEOUT)
            }
        }
    }

    // ── 만료 정리 ───────────────────────────────────────────────

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    fun cleanupExpired() {
        val expired = gameManager.expireCandidates(WAITING_TTL, ACTIVE_TTL)
        expired.forEach { game ->
            if (game.expire()) {
                log.info("action=alkkagi_expired, gameId={}, status=EXPIRED", game.id)
                broadcast(game, AlkkagiEvent.Type.EXPIRED)
            }
        }
        val purged = gameManager.purgeFinished(FINISHED_RETENTION)
        if (purged > 0) log.info("action=alkkagi_purged, count={}", purged)
    }

    private fun broadcast(
        game: AlkkagiGame,
        type: AlkkagiEvent.Type,
        flick: AlkkagiEvent.Flick? = null
    ): AlkkagiGameResponse {
        val snapshot = AlkkagiGameResponse.from(game)
        messagingTemplate.convertAndSend(
            "/topic/alkkagi/${game.id}",
            AlkkagiEvent(type = type, game = snapshot, flick = flick)
        )
        return snapshot
    }

    private fun validateGroupMember(groupRoomId: Long, userId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }
        if (groupRoom.deletedAt != null) {
            throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)
        }
        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }
    }

    companion object {
        private val WAITING_TTL: Duration = Duration.ofMinutes(10)
        private val ACTIVE_TTL: Duration = Duration.ofMinutes(60)
        private val FINISHED_RETENTION: Duration = Duration.ofMinutes(30)
        private val TURN_LIMIT_WITH_GRACE: Duration =
            Duration.ofSeconds(AlkkagiGame.TURN_LIMIT_SECONDS + 6)
    }
}
