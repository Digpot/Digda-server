package digdaserver.domain.tapbattle.domain

import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 탭배틀(연타 대결) 한 판의 인메모리 상태. 전적 미보관.
 *
 * - 1:1. 수락 시 [countdownStartAt]+3초 카운트다운 후 [battleEndAt]까지 15초 연타.
 * - 클라이언트가 주기적으로 누적 탭 수를 보고([report])하고 서버는 단조 증가로만 반영.
 * - 종료 판정은 마감 후 첫 보고/스케줄러 스윕에서 [finishIfDue] 로 확정.
 */
class TapBattleGame(
    val id: Long,
    val groupRoomId: Long,
    val inviterId: UUID,
    val inviterName: String,
    val inviteeId: UUID,
    val inviteeName: String
) {
    companion object {
        val COUNTDOWN: Duration = Duration.ofSeconds(3)
        val BATTLE_TIME: Duration = Duration.ofSeconds(15)

        /** 15초 연타의 물리적 상한(초당 20탭) — 치트 보고 클램프. */
        const val MAX_TAPS = 300
    }

    enum class Status { WAITING, ACTIVE, FINISHED, DECLINED, CANCELED, EXPIRED }

    var status: Status = Status.WAITING
        private set

    var countdownStartAt: Instant? = null
        private set
    var battleEndAt: Instant? = null
        private set

    var inviterTaps: Int = 0
        private set
    var inviteeTaps: Int = 0
        private set

    /** null 이면 무승부(FINISHED 시). */
    var winnerId: UUID? = null
        private set

    val createdAt: Instant = Instant.now()
    var lastActivityAt: Instant = Instant.now()
        private set

    fun isParticipant(userId: UUID): Boolean = userId == inviterId || userId == inviteeId

    @Synchronized
    fun accept(userId: UUID) {
        if (userId != inviteeId) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.WAITING) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        status = Status.ACTIVE
        countdownStartAt = Instant.now()
        battleEndAt = countdownStartAt!!.plus(COUNTDOWN).plus(BATTLE_TIME)
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

    /** 누적 탭 수 보고 — 단조 증가 + 상한 클램프. ACTIVE 가 아니면 무시. */
    @Synchronized
    fun report(userId: UUID, taps: Int) {
        if (!isParticipant(userId)) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.ACTIVE) return
        val clamped = taps.coerceIn(0, MAX_TAPS)
        if (userId == inviterId) {
            if (clamped > inviterTaps) inviterTaps = clamped
        } else {
            if (clamped > inviteeTaps) inviteeTaps = clamped
        }
        touch()
    }

    /** 마감이 지났으면 종료 확정 후 true. 이미 종료됐거나 아직이면 false. */
    @Synchronized
    fun finishIfDue(now: Instant): Boolean {
        val end = battleEndAt ?: return false
        if (status != Status.ACTIVE || now.isBefore(end)) return false
        status = Status.FINISHED
        winnerId = when {
            inviterTaps > inviteeTaps -> inviterId
            inviteeTaps > inviterTaps -> inviteeId
            else -> null
        }
        touch()
        return true
    }

    /** [userId] 기권 — 상대 승리로 즉시 종료. */
    @Synchronized
    fun forfeit(userId: UUID) {
        if (!isParticipant(userId)) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.ACTIVE) throw DigdaException(ErrorCode.MINIGAME_INVALID_STATE)
        status = Status.FINISHED
        winnerId = if (userId == inviterId) inviteeId else inviterId
        touch()
    }

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
}
