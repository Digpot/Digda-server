package digdaserver.domain.wordchain.domain

import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 끝말잇기 한 판의 인메모리 상태. 전적 미보관.
 *
 * 진행:
 *  1. 방장이 그룹원 여럿 초대(WAITING) → 참가/거절 → 방장이 시작(2명 이상).
 *  2. 서버가 시작 단어를 제시하고, 참가자가 턴을 돌아가며 앞 단어의 끝 글자로
 *     시작하는 단어를 제한시간 안에 잇는다(두음법칙 허용, 중복 금지, 한글 2글자+).
 *  3. 제한시간 초과 시 그 참가자는 탈락 — 마지막 남은 1명이 우승(FINISHED).
 *
 * 사전 검증은 하지 않는다(그룹끼리 자율 규칙) — 형식 규칙만 서버가 강제한다.
 */
class WordChainGame(
    val id: Long,
    val groupRoomId: Long,
    val hostId: UUID,
    val hostName: String,
    invitees: Map<UUID, String>,
    turnSeconds: Int,
    private val startWordPicker: () -> String
) {
    companion object {
        const val MIN_PLAYERS = 2
        const val MIN_TURN_SECONDS = 10
        const val MAX_TURN_SECONDS = 60
        const val DEFAULT_TURN_SECONDS = 15
        const val MAX_WORD_LENGTH = 15

        /** 한글 음절 범위. */
        private const val HANGUL_BASE = 0xAC00
        private const val HANGUL_END = 0xD7A3

        /**
         * [prev] 끝 글자를 이어받을 수 있는 시작 글자 집합 — 본 글자 + 두음법칙 변형.
         * (ㄹ→ㄴ/ㅇ, ㄴ→ㅇ: 력→역, 룡→용, 로→노, 녀→여 등)
         */
        fun allowedStarts(prev: Char): Set<Char> {
            val result = mutableSetOf(prev)
            val code = prev.code
            if (code !in HANGUL_BASE..HANGUL_END) return result
            val idx = code - HANGUL_BASE
            val initial = idx / (21 * 28)
            val medial = (idx % (21 * 28)) / 28
            val final = idx % 28
            // 중성 인덱스: ㅑ2 ㅒ3 ㅕ6 ㅖ7 ㅛ12 ㅠ17 ㅣ20 — i/y 계열.
            val yVowel = medial in setOf(2, 3, 6, 7, 12, 17, 20)
            fun compose(newInitial: Int): Char =
                (HANGUL_BASE + (newInitial * 21 + medial) * 28 + final).toChar()
            when (initial) {
                5 -> { // ㄹ
                    result.add(compose(if (yVowel) 11 else 2)) // ㅇ 또는 ㄴ
                    if (!yVowel) result.add(compose(11))
                }
                2 -> if (yVowel) result.add(compose(11)) // ㄴ→ㅇ
            }
            return result
        }

        fun isHangulWord(word: String): Boolean =
            word.isNotEmpty() && word.all { it.code in HANGUL_BASE..HANGUL_END }
    }

    enum class Status { WAITING, ACTIVE, FINISHED, CANCELED, EXPIRED }

    /** 단어 제출 결과 — ACCEPTED 외에는 턴 유지, 사유만 알려준다. */
    enum class SubmitResult { ACCEPTED, FORMAT, USED, RULE }

    class Player(
        val userId: UUID,
        val name: String,
        var joined: Boolean,
        var declined: Boolean = false,
        var eliminated: Boolean = false,
        var wordCount: Int = 0
    )

    /** 제출된 단어 기록 — [userId] null 이면 서버 제시 시작 단어. */
    data class Entry(val userId: UUID?, val userName: String?, val word: String)

    var status: Status = Status.WAITING
        private set

    val players: LinkedHashMap<UUID, Player> = LinkedHashMap()

    /** 턴 제한시간 — 방장이 설정(10~60초). */
    val turnTime: Duration =
        Duration.ofSeconds(turnSeconds.coerceIn(MIN_TURN_SECONDS, MAX_TURN_SECONDS).toLong())

    val entries: MutableList<Entry> = mutableListOf()
    private val usedWords = mutableSetOf<String>()

    var currentTurnId: UUID? = null
        private set
    var turnDeadline: Instant? = null
        private set
    var winnerId: UUID? = null
        private set

    val createdAt: Instant = Instant.now()
    var lastActivityAt: Instant = Instant.now()
        private set

    private var turnOrder: List<UUID> = emptyList()

    init {
        players[hostId] = Player(hostId, hostName, joined = true)
        invitees.forEach { (id, name) ->
            if (id != hostId) players[id] = Player(id, name, joined = false)
        }
    }

    fun isInvited(userId: UUID): Boolean = players.containsKey(userId)

    fun isJoined(userId: UUID): Boolean = players[userId]?.joined == true

    fun joinedPlayers(): List<Player> = players.values.filter { it.joined }

    fun lastWord(): String? = entries.lastOrNull()?.word

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
        turnOrder = joined.map { it.userId }.shuffled()
        val start = startWordPicker()
        entries.add(Entry(userId = null, userName = null, word = start))
        usedWords.add(start)
        currentTurnId = turnOrder.first()
        turnDeadline = Instant.now().plus(turnTime)
        touch()
    }

    /**
     * [userId] 의 단어 제출. 형식/차례가 맞으면 기록 후 다음 턴으로 넘긴다.
     * 규칙 위반은 사유([SubmitResult])만 반환하고 턴은 유지된다.
     */
    @Synchronized
    fun submit(userId: UUID, rawWord: String): SubmitResult {
        if (status != Status.ACTIVE) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        if (userId != currentTurnId) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        val p = players[userId] ?: throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        val word = rawWord.trim()
        if (word.length < 2 || word.length > MAX_WORD_LENGTH || !isHangulWord(word)) {
            return SubmitResult.FORMAT
        }
        if (word in usedWords) return SubmitResult.USED
        val prev = lastWord()
        if (prev != null && word.first() !in allowedStarts(prev.last())) {
            return SubmitResult.RULE
        }

        entries.add(Entry(userId = userId, userName = p.name, word = word))
        usedWords.add(word)
        p.wordCount += 1
        advanceTurn(from = userId)
        touch()
        return SubmitResult.ACCEPTED
    }

    /** 제한시간 초과 여부 — 스케줄러 폴링용. */
    fun turnExpired(now: Instant): Boolean {
        val deadline = turnDeadline ?: return false
        return status == Status.ACTIVE && now.isAfter(deadline)
    }

    /**
     * 현재 턴 참가자를 탈락시키고 다음 턴으로. 남은 인원이 1명이면 FINISHED 로
     * 전이하고 그 사람을 우승자로 확정. 탈락한 참가자 id 반환(없으면 null).
     */
    @Synchronized
    fun eliminateCurrent(): UUID? {
        if (status != Status.ACTIVE) return null
        val current = currentTurnId ?: return null
        players[current]?.eliminated = true
        val remaining = joinedPlayers().filter { !it.eliminated }
        if (remaining.size <= 1) {
            status = Status.FINISHED
            winnerId = remaining.firstOrNull()?.userId
            currentTurnId = null
            turnDeadline = null
        } else {
            advanceTurn(from = current)
        }
        touch()
        return current
    }

    /** [userId] 중도 포기 — 탈락 처리. 자기 턴이 아니어도 가능. */
    @Synchronized
    fun forfeit(userId: UUID) {
        if (status != Status.ACTIVE) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        val p = players[userId] ?: throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (!p.joined || p.eliminated) return
        p.eliminated = true
        val remaining = joinedPlayers().filter { !it.eliminated }
        if (remaining.size <= 1) {
            status = Status.FINISHED
            winnerId = remaining.firstOrNull()?.userId
            currentTurnId = null
            turnDeadline = null
        } else if (currentTurnId == userId) {
            advanceTurn(from = userId)
        }
        touch()
    }

    @Synchronized
    fun expire(): Boolean {
        if (status != Status.WAITING && status != Status.ACTIVE) return false
        status = Status.EXPIRED
        touch()
        return true
    }

    private fun advanceTurn(from: UUID) {
        val alive = turnOrder.filter { players[it]?.eliminated == false }
        if (alive.isEmpty()) return
        val idx = turnOrder.indexOf(from)
        // from 다음 순번부터 살아있는 첫 참가자.
        for (step in 1..turnOrder.size) {
            val candidate = turnOrder[(idx + step) % turnOrder.size]
            if (players[candidate]?.eliminated == false) {
                currentTurnId = candidate
                turnDeadline = Instant.now().plus(turnTime)
                return
            }
        }
    }

    private fun touch() {
        lastActivityAt = Instant.now()
    }
}
