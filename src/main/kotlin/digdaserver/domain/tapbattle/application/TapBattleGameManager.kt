package digdaserver.domain.tapbattle.application

import digdaserver.domain.tapbattle.domain.TapBattleGame
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** 탭배틀 인메모리 저장소 — 오목/캐치마인드와 같은 수명 정책. */
@Component
class TapBattleGameManager {

    private val log = LoggerFactory.getLogger(javaClass)

    private val games = ConcurrentHashMap<Long, TapBattleGame>()
    private val idGen = AtomicLong(System.currentTimeMillis())

    fun create(
        groupRoomId: Long,
        inviterId: UUID,
        inviterName: String,
        inviteeId: UUID,
        inviteeName: String
    ): TapBattleGame {
        val game = TapBattleGame(
            id = idGen.incrementAndGet(),
            groupRoomId = groupRoomId,
            inviterId = inviterId,
            inviterName = inviterName,
            inviteeId = inviteeId,
            inviteeName = inviteeName
        )
        games[game.id] = game
        log.info(
            "action=tapbattle_created, gameId={}, groupRoomId={}, inviterId={}, inviteeId={}",
            game.id,
            groupRoomId,
            inviterId,
            inviteeId
        )
        return game
    }

    fun get(gameId: Long): TapBattleGame =
        games[gameId] ?: throw DigdaException(ErrorCode.MINIGAME_NOT_FOUND)

    fun pendingInvitesFor(userId: UUID, groupRoomId: Long): List<TapBattleGame> =
        games.values.filter {
            it.groupRoomId == groupRoomId &&
                it.status == TapBattleGame.Status.WAITING &&
                it.inviteeId == userId
        }.sortedByDescending { it.createdAt }

    fun activeGamesFor(userId: UUID, groupRoomId: Long): List<TapBattleGame> =
        games.values.filter {
            it.groupRoomId == groupRoomId &&
                it.status == TapBattleGame.Status.ACTIVE &&
                it.isParticipant(userId)
        }.sortedByDescending { it.lastActivityAt }

    /** 마감 시각이 지난 진행 중 대결 목록 — 종료 스윕용. */
    fun dueGames(): List<TapBattleGame> {
        val now = Instant.now()
        return games.values.filter {
            it.status == TapBattleGame.Status.ACTIVE &&
                it.battleEndAt?.isBefore(now) == true
        }
    }

    fun expireCandidates(waitingTtl: Duration): List<TapBattleGame> {
        val now = Instant.now()
        return games.values.filter { game ->
            game.status == TapBattleGame.Status.WAITING &&
                Duration.between(game.createdAt, now) > waitingTtl
        }
    }

    fun purgeFinished(retention: Duration): Int {
        val now = Instant.now()
        var removed = 0
        games.values.removeIf { game ->
            val terminal = game.status != TapBattleGame.Status.WAITING &&
                game.status != TapBattleGame.Status.ACTIVE
            val stale = Duration.between(game.lastActivityAt, now) > retention
            (terminal && stale).also { if (it) removed += 1 }
        }
        return removed
    }
}
