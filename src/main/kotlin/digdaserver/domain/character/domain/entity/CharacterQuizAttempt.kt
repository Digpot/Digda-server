package digdaserver.domain.character.domain.entity

import digdaserver.domain.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
 * 한 퀴즈에 대한 한 유저의 응시 기록. (quiz, user) 유니크로 1회 응시 제한.
 *
 * 보상량은 응시 시점에 한 번 계산해 freeze (이후 곡선이 바뀌어도 과거 기록은 그대로).
 */
@Entity
@Table(
    name = "character_quiz_attempt",
    uniqueConstraints = [UniqueConstraint(columnNames = ["quiz_id", "user_id"])]
)
class CharacterQuizAttempt(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_quiz_attempt_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    val quiz: CharacterQuiz,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "selected_index", nullable = false)
    val selectedIndex: Int,

    @Column(nullable = false)
    val correct: Boolean,

    @Column(name = "earned_exp", nullable = false)
    val earnedExp: Int,

    @Column(name = "earned_coin", nullable = false)
    val earnedCoin: Int,

    @Column(name = "attempted_at", nullable = false)
    val attemptedAt: LocalDateTime = LocalDateTime.now()
)
