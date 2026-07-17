package digdaserver.domain.catchmind.application

import digdaserver.domain.catchmind.domain.CatchmindGame
import digdaserver.domain.catchmind.presentation.dto.CatchmindEvent
import digdaserver.domain.catchmind.presentation.dto.CatchmindGameResponse
import digdaserver.domain.catchmind.presentation.dto.CatchmindStrokeRequest
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
 * 캐치마인드 오케스트레이션 — 방(REST) / 진행(STOMP) / 라운드 타이머(스케줄러).
 * 이벤트는 `/topic/catchmind/{gameId}` 로 브로드캐스트. 전적은 저장하지 않는다.
 */
@Service
class CatchmindService(
    private val gameManager: CatchmindGameManager,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ── 방 (REST) ───────────────────────────────────────────────

    // 초대 알림이 notification 테이블에 INSERT 하므로 readOnly 금지 (오목 초대 회귀 참조).
    @Transactional
    fun createGame(
        hostId: UUID,
        groupRoomId: Long,
        inviteeIds: List<UUID>
    ): CatchmindGameResponse {
        val distinct = inviteeIds.distinct().filter { it != hostId }
        if (distinct.isEmpty()) throw DigdaException(ErrorCode.MINIGAME_NOT_ENOUGH_PLAYERS)
        validateGroupMember(groupRoomId, hostId)
        distinct.forEach { validateGroupMember(groupRoomId, it) }

        val host = userRepository.findById(hostId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }
        val invitees = userRepository.findAllById(distinct).associate { it.id to it.displayedName() }

        val game = gameManager.create(
            groupRoomId = groupRoomId,
            hostId = hostId,
            hostName = host.displayedName(),
            invitees = invitees
        )
        notificationService.notifyMinigameInvite(
            groupRoomId = groupRoomId,
            inviterUserId = hostId,
            inviteeUserIds = distinct,
            gameId = game.id,
            gameType = "CATCHMIND",
            gameDisplayName = "캐치마인드"
        )
        return CatchmindGameResponse.from(game, hostId)
    }

    fun getGame(userId: UUID, gameId: Long): CatchmindGameResponse {
        val game = gameManager.get(gameId)
        if (!game.isInvited(userId)) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        return CatchmindGameResponse.from(game, userId, includeStrokes = true)
    }

    fun join(userId: UUID, gameId: Long): CatchmindGameResponse {
        val game = gameManager.get(gameId)
        game.join(userId)
        log.info("action=catchmind_join, gameId={}, userId={}", gameId, userId)
        broadcastSnapshot(game, CatchmindEvent.Type.PLAYER_JOINED)
        return CatchmindGameResponse.from(game, userId, includeStrokes = true)
    }

    fun decline(userId: UUID, gameId: Long): CatchmindGameResponse {
        val game = gameManager.get(gameId)
        game.decline(userId)
        log.info("action=catchmind_decline, gameId={}, userId={}", gameId, userId)
        broadcastSnapshot(game, CatchmindEvent.Type.PLAYER_DECLINED)
        return CatchmindGameResponse.from(game, userId)
    }

    fun cancel(userId: UUID, gameId: Long): CatchmindGameResponse {
        val game = gameManager.get(gameId)
        game.cancel(userId)
        log.info("action=catchmind_cancel, gameId={}, userId={}", gameId, userId)
        broadcastSnapshot(game, CatchmindEvent.Type.CANCELED)
        return CatchmindGameResponse.from(game, userId)
    }

    fun start(userId: UUID, gameId: Long): CatchmindGameResponse {
        val game = gameManager.get(gameId)
        game.start(userId)
        log.info(
            "action=catchmind_start, gameId={}, players={}, totalRounds={}",
            gameId,
            game.joinedPlayers().size,
            game.totalRounds
        )
        broadcastSnapshot(game, CatchmindEvent.Type.STARTED)
        broadcastSnapshot(game, CatchmindEvent.Type.ROUND_START)
        return CatchmindGameResponse.from(game, userId)
    }

    // ── 진행 (STOMP) ────────────────────────────────────────────

    fun stroke(userId: UUID, gameId: Long, req: CatchmindStrokeRequest) {
        val game = gameManager.get(gameId)
        val stroke = CatchmindGame.Stroke(
            strokeId = req.strokeId,
            color = req.color,
            width = req.width,
            points = req.points,
            done = req.done
        )
        game.addStroke(userId, stroke)
        messagingTemplate.convertAndSend(
            topic(gameId),
            CatchmindEvent(type = CatchmindEvent.Type.STROKE, stroke = stroke)
        )
    }

    fun clear(userId: UUID, gameId: Long) {
        val game = gameManager.get(gameId)
        game.clearCanvas(userId)
        messagingTemplate.convertAndSend(
            topic(gameId),
            CatchmindEvent(type = CatchmindEvent.Type.CLEAR)
        )
    }

    fun skip(userId: UUID, gameId: Long) {
        val game = gameManager.get(gameId)
        val answer = game.word
        game.skipRound(userId)
        log.info("action=catchmind_skip, gameId={}, round={}", gameId, game.roundIndex)
        endRound(game, CatchmindEvent.Type.ROUND_SKIPPED, answer)
    }

    fun guess(userId: UUID, gameId: Long, text: String) {
        val game = gameManager.get(gameId)
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 40) return
        val player = game.players[userId] ?: return
        // 출제자 본인/미참가자의 채팅은 무시 — 출제자가 정답을 흘리는 것 방지.
        if (!player.joined || userId == game.drawerId) return
        val name = player.name
        val answer = game.word
        val correct = game.guess(userId, trimmed)
        if (correct) {
            log.info(
                "action=catchmind_correct, gameId={}, round={}, userId={}",
                gameId,
                game.roundIndex,
                userId
            )
            messagingTemplate.convertAndSend(
                topic(game.id),
                CatchmindEvent(
                    type = CatchmindEvent.Type.CORRECT,
                    game = CatchmindGameResponse.from(game, null),
                    userId = userId,
                    userName = name,
                    answer = answer
                )
            )
            advance(game)
        } else {
            // 오답 — 그대로 채팅으로 중계.
            messagingTemplate.convertAndSend(
                topic(game.id),
                CatchmindEvent(
                    type = CatchmindEvent.Type.GUESS,
                    userId = userId,
                    userName = name,
                    text = trimmed
                )
            )
        }
    }

    // ── 라운드 타이머 / 만료 ────────────────────────────────────

    @Scheduled(fixedDelay = 3_000, initialDelay = 15_000)
    fun tickRounds() {
        gameManager.roundExpiredGames().forEach { game ->
            log.info("action=catchmind_round_timeout, gameId={}, round={}", game.id, game.roundIndex)
            endRound(game, CatchmindEvent.Type.ROUND_TIMEOUT, game.word)
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    fun cleanupExpired() {
        val expired = gameManager.expireCandidates(WAITING_TTL, ACTIVE_TTL)
        expired.forEach { game ->
            if (game.expire()) {
                log.info("action=catchmind_expired, gameId={}", game.id)
                broadcastSnapshot(game, CatchmindEvent.Type.EXPIRED)
            }
        }
        val purged = gameManager.purgeFinished(FINISHED_RETENTION)
        if (purged > 0) log.info("action=catchmind_purged, count={}", purged)
    }

    private fun endRound(game: CatchmindGame, type: CatchmindEvent.Type, answer: String?) {
        messagingTemplate.convertAndSend(
            topic(game.id),
            CatchmindEvent(
                type = type,
                game = CatchmindGameResponse.from(game, null),
                answer = answer
            )
        )
        advance(game)
    }

    private fun advance(game: CatchmindGame) {
        val hasNext = game.advanceRound()
        if (hasNext) {
            broadcastSnapshot(game, CatchmindEvent.Type.ROUND_START)
        } else {
            log.info("action=catchmind_finished, gameId={}", game.id)
            broadcastSnapshot(game, CatchmindEvent.Type.FINISHED)
        }
    }

    private fun broadcastSnapshot(game: CatchmindGame, type: CatchmindEvent.Type) {
        messagingTemplate.convertAndSend(
            topic(game.id),
            CatchmindEvent(type = type, game = CatchmindGameResponse.from(game, null))
        )
    }

    private fun topic(gameId: Long) = "/topic/catchmind/$gameId"

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
        private val WAITING_TTL: Duration = Duration.ofMinutes(15)
        private val ACTIVE_TTL: Duration = Duration.ofMinutes(30)
        private val FINISHED_RETENTION: Duration = Duration.ofMinutes(30)
    }
}
