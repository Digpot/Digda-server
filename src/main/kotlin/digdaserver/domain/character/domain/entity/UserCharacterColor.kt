package digdaserver.domain.character.domain.entity

import digdaserver.domain.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 유저별 색상 보유. (user, color) 유니크. 같은 색을 중복 구매할 수 없다.
 *
 * 기본 색상(CORAL) 은 명시적으로 row 를 만들지 않고, 보유 여부 판정 시 항상 true 로
 * 처리한다 ([CharacterColor.isDefault]). 이걸로 빈 신규 유저에게도 row 0 으로 시작
 * 가능하게 함.
 */
@Entity
@Table(
    name = "user_character_color",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "color"])]
)
class UserCharacterColor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_character_color_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val color: CharacterColor,

    @Column(nullable = false)
    val pricePaid: Int,

    @Column(name = "acquired_at", nullable = false)
    val acquiredAt: LocalDateTime = LocalDateTime.now()
)
