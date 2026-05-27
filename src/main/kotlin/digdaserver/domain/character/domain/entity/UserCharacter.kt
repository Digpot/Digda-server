package digdaserver.domain.character.domain.entity

import digdaserver.domain.character.application.level.CharacterLevelTable
import digdaserver.domain.user.domain.entity.User
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
 * 유저 1인당 1행. 회원가입 시점에 생성하지 않고, 캐릭터 진입 시점에 lazy 생성한다
 * (퀴즈/캐릭터를 안 쓰는 유저에 대해 빈 row 를 누적하지 않으려는 목적).
 *
 * 경험치 가산은 [gainExp] 만 통해서 일어나고, 곡선/단계 갱신은 모두 그 안에서 처리한다.
 * 직접 [level], [exp] 를 set 하지 말 것.
 */
@Entity
@Table(
    name = "user_character",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id"])]
)
class UserCharacter(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_character_id")
    val id: Long = 0L,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var stage: CharacterStage = CharacterStage.EGG,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var color: CharacterColor = CharacterColor.CORAL,

    @Column(nullable = false)
    var level: Int = 1,

    @Column(nullable = false)
    var exp: Int = 0,

    @Column(nullable = false)
    var coin: Int = 0

) : BaseTimeEntity() {

    /**
     * 경험치 가산 후 레벨/스테이지를 갱신. 한 번의 호출로 여러 레벨 점프가 가능하다
     * (대량 보상 케이스). 반환값은 이번 호출로 도달한 새 레벨 수(>=0).
     *
     * 동시 호출에 대한 race 는 호출자에서 트랜잭션·잠금으로 처리.
     */
    fun gainExp(amount: Int): GainResult {
        require(amount >= 0) { "exp gain must be non-negative" }
        if (amount == 0) return GainResult(levelGained = 0, stageBefore = stage, stageAfter = stage)

        val stageBefore = stage
        var remaining = exp + amount
        var levelsGained = 0
        while (remaining >= CharacterLevelTable.expForNextLevel(level)) {
            remaining -= CharacterLevelTable.expForNextLevel(level)
            level += 1
            levelsGained += 1
            if (level >= CharacterLevelTable.MAX_LEVEL) {
                // MAX 도달 시 잉여 exp 는 0으로 고정 (저장만 누적해도 무의미한 표시 방지).
                remaining = 0
                break
            }
        }
        exp = remaining
        stage = CharacterStage.forLevel(level)
        return GainResult(levelGained = levelsGained, stageBefore = stageBefore, stageAfter = stage)
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

    fun applyColor(next: CharacterColor) {
        this.color = next
    }

    data class GainResult(
        val levelGained: Int,
        val stageBefore: CharacterStage,
        val stageAfter: CharacterStage
    ) {
        val stageChanged: Boolean get() = stageBefore != stageAfter
    }
}
