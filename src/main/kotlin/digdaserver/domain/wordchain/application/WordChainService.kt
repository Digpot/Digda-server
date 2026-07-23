package digdaserver.domain.wordchain.application

import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.domain.wordchain.domain.WordChainGame
import digdaserver.domain.wordchain.presentation.dto.WordChainEvent
import digdaserver.domain.wordchain.presentation.dto.WordChainResponse
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
 * 끝말잇기 오케스트레이션 — 방(REST) / 단어 제출(STOMP) / 턴 타이머(스케줄러).
 * 이벤트는 `/topic/wordchain/{gameId}` 로 브로드캐스트.
 */
@Service
class WordChainService(
    private val gameManager: WordChainGameManager,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 초대 알림이 notification 테이블에 INSERT 하므로 readOnly 금지.
    @Transactional
    fun createGame(
        hostId: UUID,
        groupRoomId: Long,
        inviteeIds: List<UUID>,
        turnSeconds: Int
    ): WordChainResponse {
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
            invitees = invitees,
            turnSeconds = turnSeconds
        )
        notificationService.notifyMinigameInvite(
            groupRoomId = groupRoomId,
            inviterUserId = hostId,
            inviteeUserIds = distinct,
            gameId = game.id,
            gameType = "WORD_CHAIN",
            gameDisplayName = "끝말잇기"
        )
        return WordChainResponse.from(game)
    }

    fun getGame(userId: UUID, gameId: Long): WordChainResponse {
        val game = gameManager.get(gameId)
        if (!game.isInvited(userId)) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        return WordChainResponse.from(game)
    }

    fun join(userId: UUID, gameId: Long): WordChainResponse {
        val game = gameManager.get(gameId)
        game.join(userId)
        log.info("action=wordchain_join, gameId={}, userId={}", gameId, userId)
        return broadcast(game, WordChainEvent.Type.PLAYER_JOINED)
    }

    fun decline(userId: UUID, gameId: Long): WordChainResponse {
        val game = gameManager.get(gameId)
        game.decline(userId)
        log.info("action=wordchain_decline, gameId={}, userId={}", gameId, userId)
        return broadcast(game, WordChainEvent.Type.PLAYER_DECLINED)
    }

    fun cancel(userId: UUID, gameId: Long): WordChainResponse {
        val game = gameManager.get(gameId)
        game.cancel(userId)
        log.info("action=wordchain_cancel, gameId={}, userId={}", gameId, userId)
        return broadcast(game, WordChainEvent.Type.CANCELED)
    }

    fun start(userId: UUID, gameId: Long): WordChainResponse {
        val game = gameManager.get(gameId)
        game.start(userId)
        log.info(
            "action=wordchain_start, gameId={}, players={}, turnSeconds={}",
            gameId,
            game.joinedPlayers().size,
            game.turnTime.seconds
        )
        return broadcast(game, WordChainEvent.Type.STARTED)
    }

    // ── 진행 (STOMP) ────────────────────────────────────────────

    fun submitWord(userId: UUID, gameId: Long, word: String) {
        val game = gameManager.get(gameId)
        val name = game.players[userId]?.name
        when (val result = game.submit(userId, word)) {
            WordChainGame.SubmitResult.ACCEPTED -> {
                log.info(
                    "action=wordchain_word, gameId={}, userId={}, word={}",
                    gameId,
                    userId,
                    word.trim()
                )
                messagingTemplate.convertAndSend(
                    topic(gameId),
                    WordChainEvent(
                        type = WordChainEvent.Type.WORD,
                        game = WordChainResponse.from(game),
                        userId = userId,
                        userName = name,
                        word = word.trim()
                    )
                )
            }
            else -> {
                messagingTemplate.convertAndSend(
                    topic(gameId),
                    WordChainEvent(
                        type = WordChainEvent.Type.REJECTED,
                        userId = userId,
                        userName = name,
                        word = word.trim(),
                        reason = result.name
                    )
                )
            }
        }
    }

    fun forfeit(userId: UUID, gameId: Long) {
        val game = gameManager.get(gameId)
        game.forfeit(userId)
        log.info("action=wordchain_forfeit, gameId={}, userId={}", gameId, userId)
        if (game.status == WordChainGame.Status.FINISHED) {
            broadcast(game, WordChainEvent.Type.FINISHED)
        } else {
            broadcast(game, WordChainEvent.Type.TIMEOUT)
        }
    }

    // ── 턴 타이머 / 만료 ────────────────────────────────────────

    @Scheduled(fixedDelay = 1_000, initialDelay = 10_000)
    fun tickTurns() {
        gameManager.turnExpiredGames().forEach { game ->
            val eliminated = game.eliminateCurrent() ?: return@forEach
            val name = game.players[eliminated]?.name
            log.info(
                "action=wordchain_timeout, gameId={}, eliminatedUserId={}",
                game.id,
                eliminated
            )
            if (game.status == WordChainGame.Status.FINISHED) {
                messagingTemplate.convertAndSend(
                    topic(game.id),
                    WordChainEvent(
                        type = WordChainEvent.Type.FINISHED,
                        game = WordChainResponse.from(game),
                        userId = eliminated,
                        userName = name
                    )
                )
            } else {
                messagingTemplate.convertAndSend(
                    topic(game.id),
                    WordChainEvent(
                        type = WordChainEvent.Type.TIMEOUT,
                        game = WordChainResponse.from(game),
                        userId = eliminated,
                        userName = name
                    )
                )
            }
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    fun cleanupExpired() {
        gameManager.expireCandidates(WAITING_TTL, ACTIVE_TTL).forEach { game ->
            if (game.expire()) {
                log.info("action=wordchain_expired, gameId={}", game.id)
                broadcast(game, WordChainEvent.Type.EXPIRED)
            }
        }
        val purged = gameManager.purgeFinished(FINISHED_RETENTION)
        if (purged > 0) log.info("action=wordchain_purged, count={}", purged)
    }

    private fun broadcast(game: WordChainGame, type: WordChainEvent.Type): WordChainResponse {
        val snapshot = WordChainResponse.from(game)
        messagingTemplate.convertAndSend(
            topic(game.id),
            WordChainEvent(type = type, game = snapshot)
        )
        return snapshot
    }

    private fun topic(gameId: Long) = "/topic/wordchain/$gameId"

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
