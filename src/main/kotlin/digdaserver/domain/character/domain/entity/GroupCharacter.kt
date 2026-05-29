package digdaserver.domain.character.domain.entity

import digdaserver.domain.character.application.level.CharacterLevelTable
import digdaserver.domain.group_room.domain.entity.GroupRoom
import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 그룹방 1개당 1행. 그룹원들이 함께 키우는 공용 캐릭터.
 * 그룹에서 누구든 캐릭터 화면 진입 시 lazy 생성된다.
 *
 * 경험치 가산은 [gainExp] 만 통해서 일어나고, 곡선/단계 갱신은 모두 그 안에서 처리한다.
 * 직접 [level], [exp] 를 set 하지 말 것.
 *
 * 그룹원이 퀴즈를 풀어 얻은 EXP/코인은 모두 이 한 행에 누적된다.
 *
 * 외형(색·아이템)은 이 엔티티가 직접 갖지 않고 [GroupCharacterEquipped] 에서 카테고리
 * 슬롯별로 관리한다 (스킨 1 + 모자/안경/머리핀/액세서리/잡화 각 1).
 *
 * 조력자 캐릭터 디코는 [dikoUnlocked] 플래그 하나로만 표현한다. 별도 레벨/성장 시스템이
 * 없는 동반 캐릭터라 엔티티를 새로 두지 않고 모찌 행에 같이 매단다.
 */
@Entity
@Table(
    name = "group_character",
    uniqueConstraints = [UniqueConstraint(columnNames = ["group_room_id"])]
)
class GroupCharacter(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_character_id")
    val id: Long = 0L,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_room_id", nullable = false)
    val groupRoom: GroupRoom,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var stage: CharacterStage = CharacterStage.EGG,

    @Column(nullable = false)
    var level: Int = 1,

    @Column(nullable = false)
    var exp: Int = 0,

    @Column(nullable = false)
    var coin: Int = 0,

    @Column(name = "diko_unlocked", nullable = false)
    var dikoUnlocked: Boolean = false

) : BaseTimeEntity() {

    /**
     * 경험치 가산 후 레벨/스테이지를 갱신. 한 번의 호출로 여러 레벨 점프가 가능하다
     * (대량 보상 케이스). 반환값은 이번 호출로 도달한 새 레벨 수(>=0).
     *
     * 동시 호출에 대한 race 는 호출자에서 트랜잭션·잠금으로 처리.
     *
     * 추가로, 이번 호출 도중 [DIKO_UNLOCK_LEVEL] 이상에 처음 도달하면 [dikoUnlocked] 를
     * true 로 전환하고 [GainResult.dikoJustUnlocked] 에 한 번만 true 로 반영한다.
     */
    fun gainExp(amount: Int): GainResult {
        require(amount >= 0) { "exp gain must be non-negative" }
        if (amount == 0) {
            return GainResult(
                levelGained = 0,
                stageBefore = stage,
                stageAfter = stage,
                dikoJustUnlocked = false
            )
        }

        val stageBefore = stage
        val dikoWasUnlocked = dikoUnlocked
        var remaining = exp + amount
        var levelsGained = 0
        while (remaining >= CharacterLevelTable.expForNextLevel(level)) {
            remaining -= CharacterLevelTable.expForNextLevel(level)
            level += 1
            levelsGained += 1
            if (level >= CharacterLevelTable.MAX_LEVEL) {
                remaining = 0
                break
            }
        }
        exp = remaining
        stage = CharacterStage.forLevel(level)

        val dikoJustUnlocked = if (!dikoWasUnlocked && level >= DIKO_UNLOCK_LEVEL) {
            dikoUnlocked = true
            true
        } else {
            false
        }

        return GainResult(
            levelGained = levelsGained,
            stageBefore = stageBefore,
            stageAfter = stage,
            dikoJustUnlocked = dikoJustUnlocked
        )
    }

    fun addCoin(amount: Int) {
        require(amount >= 0) { "coin gain must be non-negative" }
        coin += amount
    }

    /** 결제 성공 시 호출. 잔액 검증은 호출자(서비스 계층) 책임. */
    fun deductCoin(amount: Int) {
        require(amount >= 0) { "coin spend must be non-negative" }
        require(coin >= amount) { "insufficient coin (have=$coin, need=$amount)" }
        coin -= amount
    }

    /**
     * 어드민 보정용. 호출자(서비스 계층)가 권한·범위 검증을 끝낸 뒤에만 호출할 것.
     *
     * - [newLevel]: 1..MAX_LEVEL 사이로 호출자가 clamp 해 넘겨준다. 본 함수는 추가 검증
     *   없이 적용하고, [stage] 와 [exp] 를 새 레벨에 맞춰 자동 정합.
     *   - stage: [CharacterStage.forLevel] 로 갱신
     *   - exp: 새 레벨의 다음 임계치 미만이면 그대로 유지, 초과/MAX 도달 시 0 으로 clamp
     * - [DIKO_UNLOCK_LEVEL] 이상으로 올렸다면 [dikoUnlocked] 도 자동으로 true 로 set.
     *
     * 일반 게임 플로우에서는 [gainExp] 만 사용하고 이 메서드는 호출하지 말 것.
     */
    fun adminSetLevel(newLevel: Int) {
        require(newLevel in 1..CharacterLevelTable.MAX_LEVEL) {
            "level out of range: $newLevel"
        }
        level = newLevel
        stage = CharacterStage.forLevel(level)
        if (level >= CharacterLevelTable.MAX_LEVEL) {
            exp = 0
        } else {
            val nextThreshold = CharacterLevelTable.expForNextLevel(level)
            if (exp >= nextThreshold) exp = 0
        }
        if (level >= DIKO_UNLOCK_LEVEL) dikoUnlocked = true
    }

    /** 어드민 보정 — 코인 절대값 set. 호출자에서 비음수 검증. */
    fun adminSetCoin(newCoin: Int) {
        require(newCoin >= 0) { "coin must be non-negative" }
        coin = newCoin
    }

    /** 어드민 보정 — 디코 해금 플래그 강제 set. level<10 인 상태로 true 만들 수도 있음 (테스트용). */
    fun adminSetDikoUnlocked(unlocked: Boolean) {
        dikoUnlocked = unlocked
    }

    data class GainResult(
        val levelGained: Int,
        val stageBefore: CharacterStage,
        val stageAfter: CharacterStage,
        val dikoJustUnlocked: Boolean = false
    ) {
        val stageChanged: Boolean get() = stageBefore != stageAfter
    }

    companion object {
        /** 디코가 처음 등장하는 레벨. 모찌 본체 진화와는 별개 트리거. */
        const val DIKO_UNLOCK_LEVEL: Int = 10
    }
}
