package digdaserver.domain.tapbattle.application

import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.tapbattle.domain.TapBattleGame
import digdaserver.domain.tapbattle.presentation.dto.TapBattleEvent
import digdaserver.domain.tapbattle.presentation.dto.TapBattleResponse
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
 * 탭배틀 오케스트레이션 — 초대(REST) / 탭 보고(STOMP) / 종료·만료(스케줄러).
 * 이벤트는 `/topic/tapbattle/{gameId}` 로 브로드캐스트.
 */
@Service
class TapBattleService(
    private val gameManager: TapBattleGameManager,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 초대 알림이 notification 테이블에 INSERT 하므로 readOnly 금지.
    @Transactional
    fun createGame(inviterId: UUID, groupRoomId: Long, inviteeId: UUID): TapBattleResponse {
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
            gameType = "TAP_BATTLE",
            gameDisplayName = "탭배틀"
        )
        return TapBattleResponse.from(game)
    }

    fun getGame(userId: UUID, gameId: Long): TapBattleResponse {
        val game = gameManager.get(gameId)
        if (!game.isParticipant(userId)) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        return TapBattleResponse.from(game)
    }

    fun accept(userId: UUID, gameId: Long): TapBattleResponse {
        val game = gameManager.get(gameId)
        game.accept(userId)
        log.info("action=tapbattle_accept, gameId={}, userId={}", gameId, userId)
        return broadcast(game, TapBattleEvent.Type.ACCEPTED)
    }

    fun decline(userId: UUID, gameId: Long): TapBattleResponse {
        val game = gameManager.get(gameId)
        game.decline(userId)
        log.info("action=tapbattle_decline, gameId={}, userId={}", gameId, userId)
        return broadcast(game, TapBattleEvent.Type.DECLINED)
    }

    fun cancel(userId: UUID, gameId: Long): TapBattleResponse {
        val game = gameManager.get(gameId)
        game.cancel(userId)
        log.info("action=tapbattle_cancel, gameId={}, userId={}", gameId, userId)
        return broadcast(game, TapBattleEvent.Type.CANCELED)
    }

    // ── 진행 (STOMP) ────────────────────────────────────────────

    fun reportTaps(userId: UUID, gameId: Long, taps: Int) {
        val game = gameManager.get(gameId)
        // 마감이 지났으면 이 보고로 종료 확정.
        if (game.finishIfDue(Instant.now())) {
            log.info(
                "action=tapbattle_finished, gameId={}, inviterTaps={}, inviteeTaps={}",
                gameId,
                game.inviterTaps,
                game.inviteeTaps
            )
            broadcast(game, TapBattleEvent.Type.FINISHED)
            return
        }
        game.report(userId, taps)
        broadcast(game, TapBattleEvent.Type.SCORE)
    }

    fun forfeit(userId: UUID, gameId: Long) {
        val game = gameManager.get(gameId)
        game.forfeit(userId)
        log.info("action=tapbattle_forfeit, gameId={}, userId={}", gameId, userId)
        broadcast(game, TapBattleEvent.Type.FINISHED)
    }

    // ── 종료·만료 스윕 ──────────────────────────────────────────

    @Scheduled(fixedDelay = 2_000, initialDelay = 10_000)
    fun sweepDue() {
        gameManager.dueGames().forEach { game ->
            if (game.finishIfDue(Instant.now())) {
                log.info(
                    "action=tapbattle_finished_sweep, gameId={}, inviterTaps={}, inviteeTaps={}",
                    game.id,
                    game.inviterTaps,
                    game.inviteeTaps
                )
                broadcast(game, TapBattleEvent.Type.FINISHED)
            }
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    fun cleanupExpired() {
        gameManager.expireCandidates(WAITING_TTL).forEach { game ->
            if (game.expire()) {
                log.info("action=tapbattle_expired, gameId={}", game.id)
                broadcast(game, TapBattleEvent.Type.EXPIRED)
            }
        }
        val purged = gameManager.purgeFinished(FINISHED_RETENTION)
        if (purged > 0) log.info("action=tapbattle_purged, count={}", purged)
    }

    private fun broadcast(game: TapBattleGame, type: TapBattleEvent.Type): TapBattleResponse {
        val snapshot = TapBattleResponse.from(game)
        messagingTemplate.convertAndSend(
            "/topic/tapbattle/${game.id}",
            TapBattleEvent(type = type, game = snapshot)
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
