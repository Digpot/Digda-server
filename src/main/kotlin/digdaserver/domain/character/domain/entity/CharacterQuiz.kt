package digdaserver.domain.character.domain.entity

import digdaserver.domain.group_room.domain.entity.GroupRoom
import digdaserver.domain.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 그룹 단위 퀴즈. 작성자는 그룹 멤버여야 하며, 같은 그룹의 다른 멤버가 풀어 EXP/코인을 얻는다.
 *
 * 문제·옵션 길이/정답 인덱스 검증은 서비스 계층에서 ErrorCode 와 함께 처리한다 (엔티티는 저장 사실에만 책임).
 *
 * 그룹 인덱스를 따로 두는 이유: 그룹 안의 퀴즈 리스트 조회·랜덤 픽이 가장 흔한 쿼리 패턴.
 */
@Entity
@Table(
    name = "character_quiz",
    indexes = [Index(name = "idx_character_quiz_group", columnList = "group_room_id")]
)
class CharacterQuiz(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_quiz_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_room_id", nullable = false)
    val groupRoom: GroupRoom,

    // 작성자. 작성자가 회원탈퇴하면 NULL 로 비워 퀴즈는 보존하고 표시만 "탈퇴자"로 한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = true)
    val author: User?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val category: QuizCategory,

    @Column(nullable = false, length = 200)
    val question: String,

    @Column(name = "option1", nullable = false, length = 100)
    val option1: String,

    @Column(name = "option2", nullable = false, length = 100)
    val option2: String,

    @Column(name = "option3", nullable = false, length = 100)
    val option3: String,

    @Column(name = "option4", nullable = false, length = 100)
    val option4: String,

    @Column(name = "correct_index", nullable = false)
    val correctIndex: Int,

    @Column(name = "exp_multiplier", nullable = false)
    val expMultiplier: Int,

    /**
     * 이미지 퀴즈일 때만 채워지는 S3 URL. NULL 이면 텍스트 전용 퀴즈 (기존 호환).
     * 길이는 다른 도메인의 image URL 컬럼(2048) 과 맞춤.
     */
    @Column(name = "image_url", nullable = true, length = 2048)
    val imageUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun options(): List<String> = listOf(option1, option2, option3, option4)
}
