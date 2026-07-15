package digdaserver.domain.omok.application

import digdaserver.domain.omok.domain.OmokGame
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
 * 오목 대국 인메모리 저장소. 전적을 남기지 않으므로 DB 없이 서버 메모리에만 산다
 * (서버 재시작 시 진행 중 대국은 사라진다 — 캐주얼 미니게임으로 허용).
 *
 * 수명 관리(만료 시각 판정)는 [expireCandidates] 로 노출하고, 실제 만료 브로드캐스트는
 * 스케줄러를 가진 서비스 계층이 담당한다.
 */
@Component
class OmokGameManager {

    private val log = LoggerFactory.getLogger(javaClass)

    private val games = ConcurrentHashMap<Long, OmokGame>()
    private val idGen = AtomicLong(System.currentTimeMillis())

    fun create(
        groupRoomId: Long,
        inviterId: UUID,
        inviterName: String,
        inviteeId: UUID,
        inviteeName: String
    ): OmokGame {
        val game = OmokGame(
            id = idGen.incrementAndGet(),
            groupRoomId = groupRoomId,
            inviterId = inviterId,
            inviterName = inviterName,
            inviteeId = inviteeId,
            inviteeName = inviteeName
        )
        games[game.id] = game
        log.info(
            "action=omok_game_created, gameId={}, groupRoomId={}, inviterId={}, inviteeId={}",
            game.id,
            groupRoomId,
            inviterId,
            inviteeId
        )
        return game
    }

    fun get(gameId: Long): OmokGame =
        games[gameId] ?: throw DigdaException(ErrorCode.OMOK_GAME_NOT_FOUND)

    /** 만료 대상(대기 [waitingTtl] 초과 / 진행 [activeTtl] 방치) 대국 목록. */
    fun expireCandidates(
        waitingTtl: Duration,
        activeTtl: Duration
    ): List<OmokGame> {
        val now = Instant.now()
        return games.values.filter { game ->
            when (game.status) {
                OmokGame.Status.WAITING ->
                    Duration.between(game.createdAt, now) > waitingTtl
                OmokGame.Status.ACTIVE ->
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
            val terminal = game.status != OmokGame.Status.WAITING &&
                game.status != OmokGame.Status.ACTIVE
            val stale = Duration.between(game.lastActivityAt, now) > retention
            (terminal && stale).also { if (it) removed += 1 }
        }
        return removed
    }
}
