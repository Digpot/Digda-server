package digdaserver.domain.omok.application

import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.omok.domain.OmokGame
import digdaserver.domain.omok.presentation.dto.OmokEvent
import digdaserver.domain.omok.presentation.dto.OmokGameResponse
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
 * 오목 대전 오케스트레이션 — 초대(REST) / 진행(STOMP) / 만료(스케줄러).
 *
 * 플로우:
 *  1. 초대자가 같은 그룹 멤버 1명에게 초대 → WAITING 대국 생성, 상대에게
 *     GAME_INVITE 알림(FCM + 알림함) 발송. 초대자는 `/topic/omok/{gameId}` 구독 대기.
 *  2. 상대가 수락(accept) → ACTIVE 전환 + ACCEPTED 브로드캐스트 → 양쪽 대국 화면 진입.
 *     거절(decline)/초대 취소(cancel) 도 각각 브로드캐스트.
 *  3. 착수/기권은 STOMP 로 받고 결과(MOVE/FINISHED)를 브로드캐스트. 전적은 저장하지 않는다.
 *  4. 대기 10분 / 진행 방치 60분 초과 대국은 스케줄러가 EXPIRED 브로드캐스트 후 정리.
 */
@Service
class OmokService(
    private val gameManager: OmokGameManager,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ── 초대 (REST) ─────────────────────────────────────────────

    // 초대 알림(notifyOmokInvite)이 notification 테이블에 INSERT 하므로 readOnly 불가.
    // readOnly=true 였을 때 초대 시점에 "Connection is read-only" 로 초대가 통째로 실패했다.
    @Transactional
    fun createGame(inviterId: UUID, groupRoomId: Long, inviteeId: UUID): OmokGameResponse {
        if (inviterId == inviteeId) throw DigdaException(ErrorCode.OMOK_SELF_INVITE)
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
            inviteeName = invitee.displayedName()
        )

        notificationService.notifyOmokInvite(
            groupRoomId = groupRoomId,
            inviterUserId = inviterId,
            inviteeUserId = inviteeId,
            gameId = game.id
        )
        return OmokGameResponse.from(game)
    }

    fun getGame(userId: UUID, gameId: Long): OmokGameResponse {
        val game = gameManager.get(gameId)
        if (!game.isParticipant(userId)) throw DigdaException(ErrorCode.OMOK_NOT_PARTICIPANT)
        return OmokGameResponse.from(game)
    }

    fun accept(userId: UUID, gameId: Long): OmokGameResponse {
        val game = gameManager.get(gameId)
        game.accept(userId)
        log.info("action=omok_accept, gameId={}, userId={}", gameId, userId)
        return broadcast(game, OmokEvent.Type.ACCEPTED)
    }

    fun decline(userId: UUID, gameId: Long): OmokGameResponse {
        val game = gameManager.get(gameId)
        game.decline(userId)
        log.info("action=omok_decline, gameId={}, userId={}", gameId, userId)
        return broadcast(game, OmokEvent.Type.DECLINED)
    }

    fun cancel(userId: UUID, gameId: Long): OmokGameResponse {
        val game = gameManager.get(gameId)
        game.cancel(userId)
        log.info("action=omok_cancel, gameId={}, userId={}", gameId, userId)
        return broadcast(game, OmokEvent.Type.CANCELED)
    }

    // ── 진행 (STOMP) ────────────────────────────────────────────

    fun move(userId: UUID, gameId: Long, x: Int, y: Int) {
        val game = gameManager.get(gameId)
        val outcome = game.place(userId, x, y)
        log.info(
            "action=omok_move, gameId={}, userId={}, x={}, y={}, finished={}",
            gameId,
            userId,
            x,
            y,
            outcome.finished
        )
        val type = if (outcome.finished) OmokEvent.Type.FINISHED else OmokEvent.Type.MOVE
        broadcast(game, type, lastMove = listOf(x, y, outcome.stone))
    }

    fun forfeit(userId: UUID, gameId: Long) {
        val game = gameManager.get(gameId)
        game.forfeit(userId)
        log.info("action=omok_forfeit, gameId={}, userId={}", gameId, userId)
        broadcast(game, OmokEvent.Type.FINISHED)
    }

    // ── 만료 정리 ───────────────────────────────────────────────

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    fun cleanupExpired() {
        val expired = gameManager.expireCandidates(WAITING_TTL, ACTIVE_TTL)
        expired.forEach { game ->
            if (game.expire()) {
                log.info("action=omok_expired, gameId={}, status=EXPIRED", game.id)
                broadcast(game, OmokEvent.Type.EXPIRED)
            }
        }
        val purged = gameManager.purgeFinished(FINISHED_RETENTION)
        if (purged > 0) log.info("action=omok_purged, count={}", purged)
    }

    private fun broadcast(
        game: OmokGame,
        type: OmokEvent.Type,
        lastMove: List<Int>? = null
    ): OmokGameResponse {
        val snapshot = OmokGameResponse.from(game)
        messagingTemplate.convertAndSend(
            "/topic/omok/${game.id}",
            OmokEvent(type = type, game = snapshot, lastMove = lastMove)
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
    }
}
