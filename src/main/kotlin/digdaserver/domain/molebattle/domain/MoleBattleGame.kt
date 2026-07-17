package digdaserver.domain.molebattle.domain

import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

/**
 * 두더지 잡기 대결 한 판의 인메모리 상태. 전적 미보관.
 *
 * - 1:1. 수락 시 [spawnSeed] 를 공유 — 양쪽 클라이언트가 같은 시드로 동일한
 *   두더지 스폰 순서(위치/종류/타이밍)를 재현해 공정하게 겨룬다.
 * - 3초 카운트다운 후 30초 동안 각자 폰에서 두더지를 잡는다
 *   (일반 +1, 황금 +3, 폭탄 -2 — 채점은 클라이언트, 서버는 릴레이+클램프).
 * - 폭탄 감점이 있어 점수는 단조가 아니다 — 0..[MAX_SCORE] 범위만 강제한다.
 */
class MoleBattleGame(
    val id: Long,
    val groupRoomId: Long,
    val inviterId: UUID,
    val inviterName: String,
    val inviteeId: UUID,
    val inviteeName: String
) {
    companion object {
        val COUNTDOWN: Duration = Duration.ofSeconds(3)
        val BATTLE_TIME: Duration = Duration.ofSeconds(30)

        /** 30초 스폰 스케줄에서 나올 수 있는 이론상 최대치보다 넉넉한 상한. */
        const val MAX_SCORE = 300
    }

    enum class Status { WAITING, ACTIVE, FINISHED, DECLINED, CANCELED, EXPIRED }

    var status: Status = Status.WAITING
        private set

    /** 두더지 스폰 시드 — 수락 시 발급, 양쪽이 같은 판을 본다. */
    var spawnSeed: Long? = null
        private set

    var countdownStartAt: Instant? = null
        private set
    var battleEndAt: Instant? = null
        private set

    var inviterScore: Int = 0
        private set
    var inviteeScore: Int = 0
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
        spawnSeed = Random.nextLong(1, Long.MAX_VALUE)
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

    /** 현재 점수 보고 — 0..상한 클램프(폭탄 감점 때문에 단조 아님). ACTIVE 외 무시. */
    @Synchronized
    fun report(userId: UUID, score: Int) {
        if (!isParticipant(userId)) throw DigdaException(ErrorCode.MINIGAME_NOT_PARTICIPANT)
        if (status != Status.ACTIVE) return
        val clamped = score.coerceIn(0, MAX_SCORE)
        if (userId == inviterId) inviterScore = clamped else inviteeScore = clamped
        touch()
    }

    /** 마감이 지났으면 종료 확정 후 true. */
    @Synchronized
    fun finishIfDue(now: Instant): Boolean {
        val end = battleEndAt ?: return false
        if (status != Status.ACTIVE || now.isBefore(end)) return false
        status = Status.FINISHED
        winnerId = when {
            inviterScore > inviteeScore -> inviterId
            inviteeScore > inviterScore -> inviteeId
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
