package digdaserver.domain.omok.domain

import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import java.time.Instant
import java.util.UUID

/**
 * 오목 대국 1판의 인메모리 상태. DB 에 저장하지 않는다 (전적 미보관 정책).
 *
 * - 보드: 15×15, 0=빈칸 / 1=흑돌(초대자) / 2=백돌(수락자). 흑이 선공.
 * - 승리: 가로/세로/대각 5목 이상 (장목 허용, 렌주 금수 없음 — 캐주얼 룰).
 * - 상태 전이: WAITING → ACTIVE → FINISHED, WAITING → DECLINED/CANCELED/EXPIRED,
 *   ACTIVE → EXPIRED (장시간 방치).
 *
 * 착수는 [place] 한 곳에서만 일어나고 @Synchronized 로 직렬화된다 — 동시 착수 레이스 방지.
 */
class OmokGame(
    val id: Long,
    val groupRoomId: Long,
    val inviterId: UUID,
    val inviterName: String,
    val inviteeId: UUID,
    val inviteeName: String
) {
    companion object {
        const val BOARD_SIZE = 15
        const val BLACK = 1
        const val WHITE = 2

        /** 한 수 제한시간(초) — 초과 시 서버가 턴을 상대에게 넘긴다. */
        const val TURN_LIMIT_SECONDS = 30L
    }

    enum class Status { WAITING, ACTIVE, FINISHED, DECLINED, CANCELED, EXPIRED }

    /** 대국 종료 사유 — FIVE_IN_ROW(오목 완성) / FORFEIT(기권) / DRAW(판 가득참). */
    enum class FinishReason { FIVE_IN_ROW, FORFEIT, DRAW }

    var status: Status = Status.WAITING
        private set

    val board: Array<IntArray> = Array(BOARD_SIZE) { IntArray(BOARD_SIZE) }

    var currentTurnId: UUID = inviterId
        private set

    var winnerId: UUID? = null
        private set

    var finishReason: FinishReason? = null
        private set

    /** 승리를 완성한 다섯 돌 좌표 — 클라이언트 하이라이트용. [x, y] 목록. */
    var winningLine: List<List<Int>> = emptyList()
        private set

    val createdAt: Instant = Instant.now()

    var lastActivityAt: Instant = Instant.now()
        private set

    /** 현재 턴이 시작된 시각 — 30초 제한시간 판정 기준. */
    var turnStartedAt: Instant = Instant.now()
        private set

    private var stoneCount = 0

    fun isParticipant(userId: UUID): Boolean = userId == inviterId || userId == inviteeId

    fun stoneOf(userId: UUID): Int = if (userId == inviterId) BLACK else WHITE

    fun opponentOf(userId: UUID): UUID = if (userId == inviterId) inviteeId else inviterId

    @Synchronized
    fun accept(userId: UUID) {
        if (userId != inviteeId) throw DigdaException(ErrorCode.OMOK_NOT_PARTICIPANT)
        if (status != Status.WAITING) throw DigdaException(ErrorCode.OMOK_INVALID_STATE)
        status = Status.ACTIVE
        turnStartedAt = Instant.now()
        touch()
    }

    @Synchronized
    fun decline(userId: UUID) {
        if (userId != inviteeId) throw DigdaException(ErrorCode.OMOK_NOT_PARTICIPANT)
        if (status != Status.WAITING) throw DigdaException(ErrorCode.OMOK_INVALID_STATE)
        status = Status.DECLINED
        touch()
    }

    @Synchronized
    fun cancel(userId: UUID) {
        if (userId != inviterId) throw DigdaException(ErrorCode.OMOK_NOT_PARTICIPANT)
        if (status != Status.WAITING) throw DigdaException(ErrorCode.OMOK_INVALID_STATE)
        status = Status.CANCELED
        touch()
    }

    /**
     * [userId] 가 (x, y) 에 착수. 성공 시 (놓인 돌, 승리 여부) 를 반환하고
     * 턴을 넘기거나 대국을 종료시킨다.
     */
    @Synchronized
    fun place(userId: UUID, x: Int, y: Int): MoveOutcome {
        if (!isParticipant(userId)) throw DigdaException(ErrorCode.OMOK_NOT_PARTICIPANT)
        if (status != Status.ACTIVE) throw DigdaException(ErrorCode.OMOK_INVALID_STATE)
        if (userId != currentTurnId) throw DigdaException(ErrorCode.OMOK_INVALID_MOVE)
        if (x !in 0 until BOARD_SIZE || y !in 0 until BOARD_SIZE) {
            throw DigdaException(ErrorCode.OMOK_INVALID_MOVE)
        }
        if (board[y][x] != 0) throw DigdaException(ErrorCode.OMOK_INVALID_MOVE)

        val stone = stoneOf(userId)
        board[y][x] = stone
        stoneCount += 1
        touch()

        val line = findWinningLine(x, y, stone)
        if (line != null) {
            status = Status.FINISHED
            finishReason = FinishReason.FIVE_IN_ROW
            winnerId = userId
            winningLine = line
            return MoveOutcome(stone = stone, finished = true)
        }
        if (stoneCount >= BOARD_SIZE * BOARD_SIZE) {
            status = Status.FINISHED
            finishReason = FinishReason.DRAW
            return MoveOutcome(stone = stone, finished = true)
        }
        currentTurnId = opponentOf(userId)
        turnStartedAt = Instant.now()
        return MoveOutcome(stone = stone, finished = false)
    }

    /**
     * 현재 턴이 [limit] 을 초과했으면 턴을 상대에게 넘긴다(시간 초과 판정).
     * 방치된 대국이 lastActivityAt 를 갱신해 만료(60분)를 피하지 못하도록
     * touch() 는 하지 않는다. 넘겼으면 true.
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
        if (!isParticipant(userId)) throw DigdaException(ErrorCode.OMOK_NOT_PARTICIPANT)
        if (status != Status.ACTIVE) throw DigdaException(ErrorCode.OMOK_INVALID_STATE)
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

    private fun touch() {
        lastActivityAt = Instant.now()
    }

    /** (x, y) 에 [stone] 을 둔 직후 5목 이상이 완성됐는지 — 완성 라인 좌표 반환. */
    private fun findWinningLine(x: Int, y: Int, stone: Int): List<List<Int>>? {
        val directions = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)
        for ((dx, dy) in directions) {
            val cells = mutableListOf(listOf(x, y))
            var cx = x + dx
            var cy = y + dy
            while (inBoard(cx, cy) && board[cy][cx] == stone) {
                cells.add(listOf(cx, cy))
                cx += dx
                cy += dy
            }
            cx = x - dx
            cy = y - dy
            while (inBoard(cx, cy) && board[cy][cx] == stone) {
                cells.add(0, listOf(cx, cy))
                cx -= dx
                cy -= dy
            }
            if (cells.size >= 5) return cells
        }
        return null
    }

    private fun inBoard(x: Int, y: Int): Boolean =
        x in 0 until BOARD_SIZE && y in 0 until BOARD_SIZE

    data class MoveOutcome(val stone: Int, val finished: Boolean)
}
