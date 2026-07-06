package digdaserver.domain.nickname_exhibit.domain.entity

import digdaserver.domain.user.domain.entity.User
import digdaserver.global.common.entity.BaseTimeEntity
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

/**
 * 전시관 접근을 허용받은 사용자. 어드민이 등록/해제한다.
 * 한 사용자당 1행(unique). 행이 존재하면 모찌 화면 하단 전시관 버튼이 노출된다.
 */
@Entity
@Table(
    name = "nickname_exhibit_access",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_nickname_exhibit_access_user", columnNames = ["user_id"])
    ]
)
class NicknameExhibitAccess(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nickname_exhibit_access_id")
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User

) : BaseTimeEntity()
