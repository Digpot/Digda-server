package digdaserver.domain.molebattle.application

import digdaserver.domain.molebattle.domain.MoleBattleGame
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** 두더지 잡기 인메모리 저장소 — 다른 미니게임과 같은 수명 정책. */
@Component
class MoleBattleGameManager {

    private val log = LoggerFactory.getLogger(javaClass)

    private val games = ConcurrentHashMap<Long, MoleBattleGame>()
    private val idGen = AtomicLong(System.currentTimeMillis())

    fun create(
        groupRoomId: Long,
        inviterId: UUID,
        inviterName: String,
        inviteeId: UUID,
        inviteeName: String
    ): MoleBattleGame {
        val game = MoleBattleGame(
            id = idGen.incrementAndGet(),
            groupRoomId = groupRoomId,
            inviterId = inviterId,
            inviterName = inviterName,
            inviteeId = inviteeId,
            inviteeName = inviteeName
        )
        games[game.id] = game
        log.info(
            "action=molebattle_created, gameId={}, groupRoomId={}, inviterId={}, inviteeId={}",
            game.id,
            groupRoomId,
            inviterId,
            inviteeId
        )
        return game
    }

    fun get(gameId: Long): MoleBattleGame =
        games[gameId] ?: throw DigdaException(ErrorCode.MINIGAME_NOT_FOUND)

    fun pendingInvitesFor(userId: UUID, groupRoomId: Long): List<MoleBattleGame> =
        games.values.filter {
            it.groupRoomId == groupRoomId &&
                it.status == MoleBattleGame.Status.WAITING &&
                it.inviteeId == userId
        }.sortedByDescending { it.createdAt }

    fun activeGamesFor(userId: UUID, groupRoomId: Long): List<MoleBattleGame> =
        games.values.filter {
            it.groupRoomId == groupRoomId &&
                it.status == MoleBattleGame.Status.ACTIVE &&
                it.isParticipant(userId)
        }.sortedByDescending { it.lastActivityAt }

    fun dueGames(): List<MoleBattleGame> {
        val now = Instant.now()
        return games.values.filter {
            it.status == MoleBattleGame.Status.ACTIVE &&
                it.battleEndAt?.isBefore(now) == true
        }
    }

    fun expireCandidates(waitingTtl: Duration): List<MoleBattleGame> {
        val now = Instant.now()
        return games.values.filter { game ->
            game.status == MoleBattleGame.Status.WAITING &&
                Duration.between(game.createdAt, now) > waitingTtl
        }
    }

    fun purgeFinished(retention: Duration): Int {
        val now = Instant.now()
        var removed = 0
        games.values.removeIf { game ->
            val terminal = game.status != MoleBattleGame.Status.WAITING &&
                game.status != MoleBattleGame.Status.ACTIVE
            val stale = Duration.between(game.lastActivityAt, now) > retention
            (terminal && stale).also { if (it) removed += 1 }
        }
        return removed
    }
}
