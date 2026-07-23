package digdaserver.domain.catchmind.domain

import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 캐치마인드(그림 맞추기) 한 판의 인메모리 상태. 전적 미보관 — DB 없음.
 *
 * 진행:
 *  1. 방장이 그룹 멤버 여럿을 초대해 방 생성(WAITING). 초대받은 사람은 참가/거절.
 *  2. 방장이 시작(참가 2명 이상) → ACTIVE. 참가자가 돌아가며 출제자(drawer)가 되고,
 *     서버가 고른 단어를 그림으로 표현한다. 나머지는 채팅으로 정답 추리.
 *  3. 정답 시 맞춘 사람 +10, 출제자 +5. 라운드 제한시간(90s) 초과 시 무득점 스킵.
 *  4. 모든 라운드(참가자당 [ROUNDS_PER_PLAYER]회 출제) 소진 → FINISHED, 점수 랭킹.
 *
 * 상태 변경은 모두 @Synchronized — 동시 정답/스트로크 레이스 방지.
 */
class CatchmindGame(
    val id: Long,
    val groupRoomId: Long,
    val hostId: UUID,
    val hostName: String,
    invitees: Map<UUID, String>,
    roundSeconds: Int,
    configuredRounds: Int,
    private val wordPicker: () -> String
) {
    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_STROKES = 600
        const val MIN_ROUND_SECONDS = 30
        const val MAX_ROUND_SECONDS = 300
        const val MIN_ROUNDS = 1
        const val MAX_ROUNDS = 20
        const val DEFAULT_ROUND_SECONDS = 90
        const val DEFAULT_ROUNDS = 10
    }

    /** 방장이 방 생성 시 고른 라운드 제한시간 — 30초~5분 클램프. */
    val roundTime: Duration =
        Duration.ofSeconds(
            roundSeconds.coerceIn(MIN_ROUND_SECONDS, MAX_ROUND_SECONDS).toLong()
        )

    /** 방장이 고른 총 라운드 수 — 1~20 클램프. 출제자는 참가자 순환. */
    val configuredTotalRounds: Int =
        configuredRounds.coerceIn(MIN_ROUNDS, MAX_ROUNDS)

    enum class Status { WAITING, ACTIVE, FINISHED, CANCELED, EXPIRED }

    class Player(
        val userId: UUID,
        val name: String,
        var joined: Boolean,
        var declined: Boolean = false,
        var score: Int = 0
    )

    /** 한 획 — 정규화(0..1) 좌표 폴리라인. [done]=false 면 이어지는 획의 조각. */
    data class Stroke(
        val strokeId: Long,
        val color: String,
        val width: Double,
        val points: List<List<Double>>,
        val done: Boolean
    )

    var status: Status = Status.WAITING
        private set

    /** 방장 포함 전체 초대 대상. 방장은 항상 joined. */
    val players: LinkedHashMap<UUID, Player> = LinkedHashMap()

    var roundIndex: Int = -1
        private set
    var totalRounds: Int = 0
        private set
    var drawerId: UUID? = null
        private set
    var word: String? = null
        private set
    var roundDeadline: Instant? = null
        private set

    /** 현재 라운드의 누적 획 — 늦게 재접속한 참가자 리플레이용. */
    val strokes: MutableList<Stroke> = mutableListOf()

    val createdAt: Instant = Instant.now()
    var lastActivityAt: Instant = Instant.now()
        private set

    private var drawerOrder: List<UUID> = emptyList()
    private val usedWords = mutableSetOf<String>()

    init {
        players[hostId] = Player(hostId, hostName, joined = true)
        invitees.forEach { (id, name) ->
            if (id != hostId) players[id] = Player(id, name, joined = false)
        }
    }

    fun isInvited(userId: UUID): Boolean = players.containsKey(userId)

    fun isJoined(userId: UUID): Boolean = players[userId]?.joined == true

    fun joinedPlayers(): List<Player> = players.values.filter { it.joined }

    @Synchronized
    fun join(userId: UUID) {
        val p = players[userId] ?: throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.WAITING) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        p.joined = true
        p.declined = false
        touch()
    }

    @Synchronized
    fun decline(userId: UUID) {
        val p = players[userId] ?: throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.WAITING) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        if (userId == hostId) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        p.joined = false
        p.declined = true
        touch()
    }

    @Synchronized
    fun cancel(userId: UUID) {
        if (userId != hostId) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.WAITING) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        status = Status.CANCELED
        touch()
    }

    @Synchronized
    fun start(userId: UUID) {
        if (userId != hostId) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.WAITING) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        val joined = joinedPlayers()
        if (joined.size < MIN_PLAYERS) {
            throw DigdaException(ErrorCode.MINIGAME_NOT_ENOUGH_PLAYERS)
        }
        status = Status.ACTIVE
        drawerOrder = joined.map { it.userId }.shuffled()
        totalRounds = configuredTotalRounds
        advanceRound()
    }

    /** 다음 라운드로. 마지막 라운드였다면 FINISHED 로 전이하고 false 반환. */
    @Synchronized
    fun advanceRound(): Boolean {
        roundIndex += 1
        strokes.clear()
        touch()
        if (roundIndex >= totalRounds) {
            status = Status.FINISHED
            drawerId = null
            word = null
            roundDeadline = null
            return false
        }
        drawerId = drawerOrder[roundIndex % drawerOrder.size]
        word = pickWord()
        roundDeadline = Instant.now().plus(roundTime)
        return true
    }

    @Synchronized
    fun addStroke(userId: UUID, stroke: Stroke) {
        requireDrawer(userId)
        if (strokes.size < MAX_STROKES) strokes.add(stroke)
        touch()
    }

    @Synchronized
    fun clearCanvas(userId: UUID) {
        requireDrawer(userId)
        strokes.clear()
        touch()
    }

    /** 출제자가 이 라운드를 포기(스킵). */
    @Synchronized
    fun skipRound(userId: UUID) {
        requireDrawer(userId)
        touch()
    }

    /**
     * [userId] 의 추리. 정답이면 점수 반영 후 true — 호출자가 advanceRound 를 이어간다.
     * 출제자 본인의 채팅은 추리로 치지 않는다.
     */
    @Synchronized
    fun guess(userId: UUID, text: String): Boolean {
        if (status != Status.ACTIVE) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        val p = players[userId] ?: throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (!p.joined || userId == drawerId) return false
        val answer = word ?: return false
        val correct = normalize(text) == normalize(answer)
        if (correct) {
            p.score += 10
            drawerId?.let { players[it]?.let { d -> d.score += 5 } }
        }
        touch()
        return correct
    }

    /** 라운드 제한시간 초과 여부 — 만료 스케줄러가 폴링. */
    fun roundExpired(now: Instant): Boolean {
        val deadline = roundDeadline ?: return false
        return status == Status.ACTIVE && now.isAfter(deadline)
    }

    @Synchronized
    fun expire(): Boolean {
        if (status != Status.WAITING && status != Status.ACTIVE) return false
        status = Status.EXPIRED
        touch()
        return true
    }

    private fun requireDrawer(userId: UUID) {
        if (status != Status.ACTIVE) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        if (userId != drawerId) throw DigdaException(ErrorCode.MINIGAME_NOT_DRAWER)
    }

    private fun pickWord(): String {
        // 중복 회피 시도 — 단어가 소진되면 재사용 허용.
        repeat(20) {
            val w = wordPicker()
            if (usedWords.add(w)) return w
        }
        return wordPicker()
    }

    private fun touch() {
        lastActivityAt = Instant.now()
    }

    private fun normalize(s: String): String = s.replace(" ", "").trim().lowercase()
}
