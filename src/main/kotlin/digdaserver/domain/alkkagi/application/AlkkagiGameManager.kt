package digdaserver.domain.alkkagi.application

import digdaserver.domain.alkkagi.domain.AlkkagiGame
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 알까기 대국 인메모리 저장소 — 오목([digdaserver.domain.omok.application.OmokGameManager])
 * 과 동일한 수명 정책. 전적을 남기지 않으므로 서버 재시작 시 진행 중 대국은 사라진다.
 */
@Component
class AlkkagiGameManager {

    private val log = LoggerFactory.getLogger(javaClass)

    private val games = ConcurrentHashMap<Long, AlkkagiGame>()
    private val idGen = AtomicLong(System.currentTimeMillis())

    fun create(
        groupRoomId: Long,
        inviterId: UUID,
        inviterName: String,
        inviteeId: UUID,
        inviteeName: String,
        stoneCount: Int
    ): AlkkagiGame {
        val game = AlkkagiGame(
            id = idGen.incrementAndGet(),
            groupRoomId = groupRoomId,
            inviterId = inviterId,
            inviterName = inviterName,
            inviteeId = inviteeId,
            inviteeName = inviteeName,
            stoneCountRequested = stoneCount
        )
        games[game.id] = game
        log.info(
            "action=alkkagi_game_created, gameId={}, groupRoomId={}, inviterId={}, inviteeId={}, stoneCount={}",
            game.id,
            groupRoomId,
            inviterId,
            inviteeId,
            game.stoneCount
        )
        return game
    }

    fun get(gameId: Long): AlkkagiGame =
        games[gameId] ?: throw DigdaException(ErrorCode.MINIGAME_NOT_FOUND)

    /** [groupRoomId] 그룹에서 [userId] 가 초대받고 아직 응답 안 한 대기 대국 목록. */
    fun pendingInvitesFor(userId: UUID, groupRoomId: Long): List<AlkkagiGame> =
        games.values.filter {
            it.groupRoomId == groupRoomId &&
                it.status == AlkkagiGame.Status.WAITING &&
                it.inviteeId == userId
        }.sortedByDescending { it.createdAt }

    /** [groupRoomId] 그룹에서 [userId] 가 참여 중인 진행(ACTIVE) 대국 목록 — 재입장용. */
    fun activeGamesFor(userId: UUID, groupRoomId: Long): List<AlkkagiGame> =
        games.values.filter {
            it.groupRoomId == groupRoomId &&
                it.status == AlkkagiGame.Status.ACTIVE &&
                it.isParticipant(userId)
        }.sortedByDescending { it.lastActivityAt }

    /** 진행(ACTIVE) 중인 전체 대국 — 턴 제한시간 스케줄러용. */
    fun allActive(): List<AlkkagiGame> =
        games.values.filter { it.status == AlkkagiGame.Status.ACTIVE }

    /** 만료 대상(대기 [waitingTtl] 초과 / 진행 [activeTtl] 방치) 대국 목록. */
    fun expireCandidates(
        waitingTtl: Duration,
        activeTtl: Duration
    ): List<AlkkagiGame> {
        val now = Instant.now()
        return games.values.filter { game ->
            when (game.status) {
                AlkkagiGame.Status.WAITING ->
                    Duration.between(game.createdAt, now) > waitingTtl
                AlkkagiGame.Status.ACTIVE ->
                    Duration.between(game.lastActivityAt, now) > activeTtl
                else -> false
            }
        }
    }

    /** 종료된 지 [retention] 이 지난 대국을 메모리에서 제거. 제거 수 반환. */
    fun purgeFinished(retention: Duration): Int {
        val now = Instant.now()
        var removed = 0
        games.values.removeIf { game ->
            val terminal = game.status != AlkkagiGame.Status.WAITING &&
                game.status != AlkkagiGame.Status.ACTIVE
            val stale = Duration.between(game.lastActivityAt, now) > retention
            (terminal && stale).also { if (it) removed += 1 }
        }
        return removed
    }
}
