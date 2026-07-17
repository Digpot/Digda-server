package digdaserver.domain.molebattle.application

import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.molebattle.domain.MoleBattleGame
import digdaserver.domain.molebattle.presentation.dto.MoleBattleEvent
import digdaserver.domain.molebattle.presentation.dto.MoleBattleResponse
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
import java.time.Instant
import java.util.UUID

/**
 * 두더지 잡기 오케스트레이션 — 초대(REST) / 점수 보고(STOMP) / 종료·만료(스케줄러).
 * 이벤트는 `/topic/molebattle/{gameId}` 로 브로드캐스트.
 */
@Service
class MoleBattleService(
    private val gameManager: MoleBattleGameManager,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 초대 알림이 notification 테이블에 INSERT 하므로 readOnly 금지.
    @Transactional
    fun createGame(inviterId: UUID, groupRoomId: Long, inviteeId: UUID): MoleBattleResponse {
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
            inviteeName = invitee.displayedName()
        )
        notificationService.notifyMinigameInvite(
            groupRoomId = groupRoomId,
            inviterUserId = inviterId,
            inviteeUserIds = listOf(inviteeId),
            gameId = game.id,
            gameType = "MOLE_BATTLE",
            gameDisplayName = "두더지 잡기"
        )
        return MoleBattleResponse.from(game)
    }

    fun getGame(userId: UUID, gameId: Long): MoleBattleResponse {
        val game = gameManager.get(gameId)
        if (!game.isParticipant(userId)) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        return MoleBattleResponse.from(game)
    }

    fun accept(userId: UUID, gameId: Long): MoleBattleResponse {
        val game = gameManager.get(gameId)
        game.accept(userId)
        log.info("action=molebattle_accept, gameId={}, userId={}", gameId, userId)
        return broadcast(game, MoleBattleEvent.Type.ACCEPTED)
    }

    fun decline(userId: UUID, gameId: Long): MoleBattleResponse {
        val game = gameManager.get(gameId)
        game.decline(userId)
        log.info("action=molebattle_decline, gameId={}, userId={}", gameId, userId)
        return broadcast(game, MoleBattleEvent.Type.DECLINED)
    }

    fun cancel(userId: UUID, gameId: Long): MoleBattleResponse {
        val game = gameManager.get(gameId)
        game.cancel(userId)
        log.info("action=molebattle_cancel, gameId={}, userId={}", gameId, userId)
        return broadcast(game, MoleBattleEvent.Type.CANCELED)
    }

    // ── 진행 (STOMP) ────────────────────────────────────────────

    fun reportScore(userId: UUID, gameId: Long, score: Int) {
        val game = gameManager.get(gameId)
        if (game.finishIfDue(Instant.now())) {
            log.info(
                "action=molebattle_finished, gameId={}, inviterScore={}, inviteeScore={}",
                gameId,
                game.inviterScore,
                game.inviteeScore
            )
            broadcast(game, MoleBattleEvent.Type.FINISHED)
            return
        }
        game.report(userId, score)
        broadcast(game, MoleBattleEvent.Type.SCORE)
    }

    fun forfeit(userId: UUID, gameId: Long) {
        val game = gameManager.get(gameId)
        game.forfeit(userId)
        log.info("action=molebattle_forfeit, gameId={}, userId={}", gameId, userId)
        broadcast(game, MoleBattleEvent.Type.FINISHED)
    }

    // ── 종료·만료 스윕 ──────────────────────────────────────────

    @Scheduled(fixedDelay = 2_000, initialDelay = 12_000)
    fun sweepDue() {
        gameManager.dueGames().forEach { game ->
            if (game.finishIfDue(Instant.now())) {
                log.info(
                    "action=molebattle_finished_sweep, gameId={}, inviterScore={}, inviteeScore={}",
                    game.id,
                    game.inviterScore,
                    game.inviteeScore
                )
                broadcast(game, MoleBattleEvent.Type.FINISHED)
            }
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    fun cleanupExpired() {
        gameManager.expireCandidates(WAITING_TTL).forEach { game ->
            if (game.expire()) {
                log.info("action=molebattle_expired, gameId={}", game.id)
                broadcast(game, MoleBattleEvent.Type.EXPIRED)
            }
        }
        val purged = gameManager.purgeFinished(FINISHED_RETENTION)
        if (purged > 0) log.info("action=molebattle_purged, count={}", purged)
    }

    private fun broadcast(game: MoleBattleGame, type: MoleBattleEvent.Type): MoleBattleResponse {
        val snapshot = MoleBattleResponse.from(game)
        messagingTemplate.convertAndSend(
            "/topic/molebattle/${game.id}",
            MoleBattleEvent(type = type, game = snapshot)
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
        private val FINISHED_RETENTION: Duration = Duration.ofMinutes(30)
    }
}
