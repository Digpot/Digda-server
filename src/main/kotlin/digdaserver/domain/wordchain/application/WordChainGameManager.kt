package digdaserver.domain.wordchain.application

import digdaserver.domain.wordchain.domain.WordChainGame
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/** 끝말잇기 인메모리 저장소 + 시작 단어 사전 — 다른 미니게임과 같은 수명 정책. */
@Component
class WordChainGameManager {

    private val log = LoggerFactory.getLogger(javaClass)

    private val games = ConcurrentHashMap<Long, WordChainGame>()
    private val idGen = AtomicLong(System.currentTimeMillis())

    fun create(
        groupRoomId: Long,
        hostId: UUID,
        hostName: String,
        invitees: Map<UUID, String>,
        turnSeconds: Int
    ): WordChainGame {
        val game = WordChainGame(
            id = idGen.incrementAndGet(),
            groupRoomId = groupRoomId,
            hostId = hostId,
            hostName = hostName,
            invitees = invitees,
            turnSeconds = turnSeconds,
            startWordPicker = { START_WORDS[Random.nextInt(START_WORDS.size)] }
        )
        games[game.id] = game
        log.info(
            "action=wordchain_created, gameId={}, groupRoomId={}, hostId={}, invitees={}, turnSeconds={}",
            game.id,
            groupRoomId,
            hostId,
            invitees.size,
            game.turnTime.seconds
        )
        return game
    }

    fun get(gameId: Long): WordChainGame =
        games[gameId] ?: throw DigdaException(ErrorCode.MINIGAME_NOT_FOUND)

    fun pendingInvitesFor(userId: UUID, groupRoomId: Long): List<WordChainGame> =
        games.values.filter { g ->
            g.groupRoomId == groupRoomId &&
                g.status == WordChainGame.Status.WAITING &&
                g.players[userId]?.let { !it.joined && !it.declined && it.userId != g.hostId } == true
        }.sortedByDescending { it.createdAt }

    fun activeGamesFor(userId: UUID, groupRoomId: Long): List<WordChainGame> =
        games.values.filter { g ->
            g.groupRoomId == groupRoomId &&
                (g.status == WordChainGame.Status.WAITING || g.status == WordChainGame.Status.ACTIVE) &&
                g.isJoined(userId)
        }.sortedByDescending { it.lastActivityAt }

    /** 턴 제한시간이 지난 진행 중 게임 목록. */
    fun turnExpiredGames(): List<WordChainGame> {
        val now = Instant.now()
        return games.values.filter { it.turnExpired(now) }
    }

    fun expireCandidates(waitingTtl: Duration, activeTtl: Duration): List<WordChainGame> {
        val now = Instant.now()
        return games.values.filter { game ->
            when (game.status) {
                WordChainGame.Status.WAITING ->
                    Duration.between(game.createdAt, now) > waitingTtl
                WordChainGame.Status.ACTIVE ->
                    Duration.between(game.lastActivityAt, now) > activeTtl
                else -> false
            }
        }
    }

    fun purgeFinished(retention: Duration): Int {
        val now = Instant.now()
        var removed = 0
        games.values.removeIf { game ->
            val terminal = game.status != WordChainGame.Status.WAITING &&
                game.status != WordChainGame.Status.ACTIVE
            val stale = Duration.between(game.lastActivityAt, now) > retention
            (terminal && stale).also { if (it) removed += 1 }
        }
        return removed
    }

    companion object {
        /** 시작 단어 사전 — 흔한 두 글자+ 명사, 잇기 좋은 끝 글자 위주. */
        val START_WORDS: List<String> = listOf(
            "사과", "나무", "바다", "구름", "하늘", "기차", "자전거", "노래",
            "친구", "학교", "가방", "연필", "지도", "여행", "공원", "시계",
            "라면", "김밥", "치킨", "피자", "딸기", "수박", "포도", "바나나",
            "강아지", "고양이", "토끼", "기린", "판다", "두더지", "고래", "나비",
            "무지개", "별빛", "우산", "장미", "소풍", "운동", "축제", "보물"
        )
    }
}
