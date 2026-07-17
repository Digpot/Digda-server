package digdaserver.domain.catchmind.application

import digdaserver.domain.catchmind.domain.CatchmindGame
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

/**
 * 캐치마인드 게임 인메모리 저장소 + 출제 단어 사전.
 * 오목([digdaserver.domain.omok.application.OmokGameManager])과 같은 수명 정책 —
 * 전적 미보관, 서버 재시작 시 소멸.
 */
@Component
class CatchmindGameManager {

    private val log = LoggerFactory.getLogger(javaClass)

    private val games = ConcurrentHashMap<Long, CatchmindGame>()
    private val idGen = AtomicLong(System.currentTimeMillis())

    fun create(
        groupRoomId: Long,
        hostId: UUID,
        hostName: String,
        invitees: Map<UUID, String>,
        roundSeconds: Int,
        totalRounds: Int
    ): CatchmindGame {
        val game = CatchmindGame(
            id = idGen.incrementAndGet(),
            groupRoomId = groupRoomId,
            hostId = hostId,
            hostName = hostName,
            invitees = invitees,
            roundSeconds = roundSeconds,
            configuredRounds = totalRounds,
            wordPicker = { WORDS[Random.nextInt(WORDS.size)] }
        )
        games[game.id] = game
        log.info(
            "action=catchmind_created, gameId={}, groupRoomId={}, hostId={}, invitees={}",
            game.id,
            groupRoomId,
            hostId,
            invitees.size
        )
        return game
    }

    fun get(gameId: Long): CatchmindGame =
        games[gameId] ?: throw DigdaException(ErrorCode.MINIGAME_NOT_FOUND)

    /** [groupRoomId] 그룹에서 [userId] 가 초대받고 아직 응답 안 한 대기 방 목록. */
    fun pendingInvitesFor(userId: UUID, groupRoomId: Long): List<CatchmindGame> =
        games.values.filter { g ->
            g.groupRoomId == groupRoomId &&
                g.status == CatchmindGame.Status.WAITING &&
                g.players[userId]?.let { !it.joined && !it.declined && it.userId != g.hostId } == true
        }.sortedByDescending { it.createdAt }

    /** [groupRoomId] 그룹에서 [userId] 가 참가한 대기/진행 방 목록 — 재입장용. */
    fun activeGamesFor(userId: UUID, groupRoomId: Long): List<CatchmindGame> =
        games.values.filter { g ->
            g.groupRoomId == groupRoomId &&
                (g.status == CatchmindGame.Status.WAITING || g.status == CatchmindGame.Status.ACTIVE) &&
                g.isJoined(userId)
        }.sortedByDescending { it.lastActivityAt }

    /** 라운드 제한시간이 지난 진행 중 게임 목록. */
    fun roundExpiredGames(): List<CatchmindGame> {
        val now = Instant.now()
        return games.values.filter { it.roundExpired(now) }
    }

    fun expireCandidates(waitingTtl: Duration, activeTtl: Duration): List<CatchmindGame> {
        val now = Instant.now()
        return games.values.filter { game ->
            when (game.status) {
                CatchmindGame.Status.WAITING ->
                    Duration.between(game.createdAt, now) > waitingTtl
                CatchmindGame.Status.ACTIVE ->
                    Duration.between(game.lastActivityAt, now) > activeTtl
                else -> false
            }
        }
    }

    fun purgeFinished(retention: Duration): Int {
        val now = Instant.now()
        var removed = 0
        games.values.removeIf { game ->
            val terminal = game.status != CatchmindGame.Status.WAITING &&
                game.status != CatchmindGame.Status.ACTIVE
            val stale = Duration.between(game.lastActivityAt, now) > retention
            (terminal && stale).also { if (it) removed += 1 }
        }
        return removed
    }

    companion object {
        /** 출제 단어 사전 — 그림으로 표현하기 좋은 명사 위주. */
        val WORDS: List<String> = listOf(
            "사과", "바나나", "수박", "포도", "딸기", "복숭아", "파인애플", "귤",
            "강아지", "고양이", "토끼", "코끼리", "기린", "펭귄", "돼지", "사자",
            "호랑이", "판다", "두더지", "다람쥐", "고래", "문어", "나비", "꿀벌",
            "자동차", "버스", "기차", "비행기", "배", "자전거", "오토바이", "헬리콥터",
            "집", "학교", "병원", "교회", "빌딩", "다리", "등대", "성",
            "해", "달", "별", "구름", "비", "눈사람", "무지개", "번개",
            "나무", "꽃", "선인장", "버섯", "단풍잎", "야자수",
            "피자", "햄버거", "치킨", "라면", "김밥", "떡볶이", "아이스크림", "케이크",
            "커피", "우유", "주스", "붕어빵", "솜사탕", "핫도그",
            "축구공", "야구방망이", "농구공", "탁구채", "스케이트", "낚싯대",
            "기타", "피아노", "드럼", "바이올린", "마이크", "트럼펫",
            "안경", "모자", "장갑", "우산", "가방", "신발", "양말", "넥타이",
            "시계", "휴대폰", "컴퓨터", "텔레비전", "카메라", "로봇", "드론",
            "칫솔", "비누", "가위", "연필", "책", "의자", "침대", "거울",
            "열쇠", "촛불", "풍선", "선물상자", "왕관", "반지",
            "유령", "천사", "공룡", "외계인", "마법사", "해적",
            "소방차", "구급차", "경찰차", "로켓", "잠수함", "케이블카"
        )
    }
}
