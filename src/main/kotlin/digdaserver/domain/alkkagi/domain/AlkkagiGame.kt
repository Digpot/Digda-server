package digdaserver.domain.alkkagi.domain

import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import java.time.Instant
import java.util.UUID

/**
 * 알까기 대국 1판의 인메모리 상태. DB 에 저장하지 않는다 (전적 미보관 정책 — 오목과 동일).
 *
 * - 보드: 정사각형, 좌표는 0..1 정규화. 초대자 돌은 아래(y≈0.85), 수락자 돌은 위(y≈0.15).
 * - 돌 개수: 초대자가 초대 시 1~10개로 설정([stoneCount]) — 양쪽 동일 개수로 시작.
 * - 물리 시뮬레이션은 클라이언트가 수행하고, 서버는 "턴 검증 + 결과 반영 + 승패 판정 +
 *   브로드캐스트"만 담당한다(캐주얼 룰 — 오목과 같은 신뢰 모델).
 * - 승리: 상대 돌이 전부 판 밖으로 나가면 승리. 내 마지막 돌이 함께 나가 양쪽 모두
 *   0개가 되면 그 수를 둔 쪽이 패배한다(자폭 룰).
 *
 * 상태 전이는 오목과 동일: WAITING → ACTIVE → FINISHED, WAITING → DECLINED/CANCELED/
 * EXPIRED, ACTIVE → EXPIRED. 수 반영은 [applyMove] 한 곳에서 @Synchronized 로 직렬화된다.
 */
class AlkkagiGame(
    val id: Long,
    val groupRoomId: Long,
    val inviterId: UUID,
    val inviterName: String,
    val inviteeId: UUID,
    val inviteeName: String,
    stoneCountRequested: Int
) {
    companion object {
        const val MIN_STONES = 1
        const val MAX_STONES = 10
        const val INVITER = 1
        const val INVITEE = 2

        /** 한 수 제한시간(초) — 초과 시 서버가 턴을 상대에게 넘긴다. */
        const val TURN_LIMIT_SECONDS = 30L
    }

    enum class Status { WAITING, ACTIVE, FINISHED, DECLINED, CANCELED, EXPIRED }

    /** 종료 사유 — KNOCKOUT(상대 돌 전멸) / FORFEIT(기권). */
    enum class FinishReason { KNOCKOUT, FORFEIT }

    /** 돌 하나 — 좌표는 0..1 정규화, [alive]=false 면 판 밖으로 나간 돌. */
    data class Stone(
        val id: Int,
        val owner: Int,
        var x: Double,
        var y: Double,
        var alive: Boolean = true
    )

    /** 클라이언트가 보내는 수 반영 페이로드 — 시뮬레이션 종료 후의 돌 최종 상태. */
    data class StoneUpdate(val id: Int, val x: Double, val y: Double, val alive: Boolean)

    val stoneCount: Int = stoneCountRequested.coerceIn(MIN_STONES, MAX_STONES)

    var status: Status = Status.WAITING
        private set

    val stones: MutableList<Stone> = buildInitialStones(stoneCount)

    var currentTurnId: UUID = inviterId
        private set

    var winnerId: UUID? = null
        private set

    var finishReason: FinishReason? = null
        private set

    val createdAt: Instant = Instant.now()

    var lastActivityAt: Instant = Instant.now()
        private set

    /** 현재 턴이 시작된 시각 — 30초 제한시간 판정 기준. */
    var turnStartedAt: Instant = Instant.now()
        private set

    fun isParticipant(userId: UUID): Boolean = userId == inviterId || userId == inviteeId

    fun sideOf(userId: UUID): Int = if (userId == inviterId) INVITER else INVITEE

    fun opponentOf(userId: UUID): UUID = if (userId == inviterId) inviteeId else inviterId

    @Synchronized
    fun accept(userId: UUID) {
        if (userId != inviteeId) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.WAITING) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        status = Status.ACTIVE
        turnStartedAt = Instant.now()
        touch()
    }

    @Synchronized
    fun decline(userId: UUID) {
        if (userId != inviteeId) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.WAITING) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        status = Status.DECLINED
        touch()
    }

    @Synchronized
    fun cancel(userId: UUID) {
        if (userId != inviterId) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.WAITING) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        status = Status.CANCELED
        touch()
    }

    /**
     * [userId] 가 [stoneId] 돌을 튕긴 결과([updates])를 반영한다.
     *
     * 검증: 참가자 / ACTIVE / 내 턴 / 내 살아있는 돌. 위치는 클라이언트 시뮬 결과를
     * 신뢰하되, 죽은 돌의 부활(alive false→true)만은 막는다. 반영 후 승패를 판정하고
     * 턴을 넘기거나 대국을 종료시킨다. 종료 여부를 반환한다.
     */
    @Synchronized
    fun applyMove(userId: UUID, stoneId: Int, updates: List<StoneUpdate>): Boolean {
        if (!isParticipant(userId)) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.ACTIVE) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        if (userId != currentTurnId) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        val flicked = stones.firstOrNull { it.id == stoneId }
            ?: throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        if (!flicked.alive || flicked.owner != sideOf(userId)) {
            throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        }

        val byId = updates.associateBy { it.id }
        stones.forEach { stone ->
            val u = byId[stone.id] ?: return@forEach
            stone.x = u.x.coerceIn(-0.5, 1.5)
            stone.y = u.y.coerceIn(-0.5, 1.5)
            // 죽은 돌은 되살릴 수 없다 — 그 외에는 클라 시뮬 결과를 신뢰.
            if (stone.alive) stone.alive = u.alive
        }
        touch()

        val myAlive = stones.count { it.owner == sideOf(userId) && it.alive }
        val oppAlive = stones.count { it.owner != sideOf(userId) && it.alive }
        when {
            oppAlive == 0 && myAlive > 0 -> finishKnockout(winner = userId)
            myAlive == 0 && oppAlive > 0 -> finishKnockout(winner = opponentOf(userId))
            // 동시 전멸 — 수를 둔 쪽의 자폭 패.
            oppAlive == 0 && myAlive == 0 -> finishKnockout(winner = opponentOf(userId))
            else -> {
                currentTurnId = opponentOf(userId)
                turnStartedAt = Instant.now()
            }
        }
        return status == Status.FINISHED
    }

    /**
     * 현재 턴이 [limit] 을 초과했으면 턴을 상대에게 넘긴다(시간 초과 판정).
     * 방치된 대국이 만료(60분)를 피하지 못하도록 touch() 는 하지 않는다.
     */
    @Synchronized
    fun timeoutTurnIfExpired(limit: java.time.Duration): Boolean {
        if (status != Status.ACTIVE) return false
        if (java.time.Duration.between(turnStartedAt, Instant.now()) <= limit) return false
        currentTurnId = opponentOf(currentTurnId)
        turnStartedAt = Instant.now()
        return true
    }

    /** [userId] 기권 — 상대 승리로 즉시 종료. */
    @Synchronized
    fun forfeit(userId: UUID) {
        if (!isParticipant(userId)) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.ACTIVE) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        status = Status.FINISHED
        finishReason = FinishReason.FORFEIT
        winnerId = opponentOf(userId)
        touch()
    }

    /** 청소 스케줄러가 만료 처리할 때 호출. */
    @Synchronized
    fun expire(): Boolean {
        if (status != Status.WAITING && status != Status.ACTIVE) return false
        status = Status.EXPIRED
        touch()
        return true
    }

    private fun finishKnockout(winner: UUID) {
        status = Status.FINISHED
        finishReason = FinishReason.KNOCKOUT
        winnerId = winner
    }

    private fun touch() {
        lastActivityAt = Instant.now()
    }

    private fun buildInitialStones(count: Int): MutableList<Stone> {
        val stones = mutableListOf<Stone>()
        var nextId = 0

        // 한 줄 최대 5개 — 6개 이상이면 두 번째 줄을 판 가장자리 쪽에 깐다.
        fun layRows(owner: Int, frontY: Double, backY: Double) {
            val front = minOf(count, 5)
            val back = count - front
            fun rowXs(k: Int): List<Double> =
                (0 until k).map { 0.5 + (it - (k - 1) / 2.0) * 0.13 }
            rowXs(front).forEach { x ->
                stones.add(Stone(id = nextId++, owner = owner, x = x, y = frontY))
            }
            if (back > 0) {
                rowXs(back).forEach { x ->
                    stones.add(Stone(id = nextId++, owner = owner, x = x, y = backY))
                }
            }
        }
        layRows(INVITER, frontY = 0.82, backY = 0.92)
        layRows(INVITEE, frontY = 0.18, backY = 0.08)
        return stones
    }
}
