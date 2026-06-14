package digdaserver.domain.user.domain.entity

import digdaserver.domain.oauth2.domain.entity.SocialProvider
import digdaserver.global.common.entity.BaseTimeEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(
    name = "user",
    uniqueConstraints = [UniqueConstraint(columnNames = ["social_id", "social_provider"])]
)
class User(

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "social_id")
    val socialId: String? = null,

    var email: String?,

    // 소셜 로그인에서 받아온 원본 이름. 가입 시 1회 설정되며 프로필 편집으로는 바뀌지 않는다.
    @Column(nullable = false, length = 20)
    var name: String,

    // 사용자가 프로필 편집에서 직접 지정한 표시 이름. null 이면 소셜 원본(name)을 그대로 노출.
    @Column(name = "display_name", length = 20)
    var displayName: String? = null,

    @Column(name = "profile_image")
    var profileImage: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false)
    val socialProvider: SocialProvider,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER,

    // 서비스 이용 제한 — true 면 앱에서 마이페이지 외 기능을 막는다(어드민이 설정).
    @Column(name = "restricted", nullable = false)
    var restricted: Boolean = false

) : BaseTimeEntity() {

    @OneToOne(
        mappedBy = "user",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var terms: UserTerms? = null
        protected set

    @OneToOne(
        mappedBy = "user",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var notificationSetting: UserNotificationSetting? = null
        protected set

    @OneToOne(
        mappedBy = "user",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var privacySetting: UserPrivacySetting? = null
        protected set

    /** 화면에 노출할 이름 — 사용자가 지정한 표시 이름이 있으면 그것을, 없으면 소셜 원본 이름을 쓴다. */
    fun displayedName(): String = displayName ?: name

    fun updateProfile(name: String?, profileImage: String?) {
        // 프로필 편집은 원본 name 을 건드리지 않고 표시 이름(displayName)만 갱신한다.
        name?.let { this.displayName = it }
        profileImage?.let { this.profileImage = it }
    }

    fun resetProfileImage() {
        this.profileImage = null
    }

    fun agreeToTerms(terms: UserTerms) {
        this.terms = terms
    }

    fun initNotificationSetting(setting: UserNotificationSetting) {
        this.notificationSetting = setting
    }

    fun initPrivacySetting(setting: UserPrivacySetting) {
        this.privacySetting = setting
    }

    /** 자격증명 없는 유령 ADMIN 정리용 — 역할만 USER 로 강등(데이터 삭제 아님). */
    fun demoteToUser() {
        this.role = Role.USER
    }

    /** 서비스 이용 제한 설정/해제 (어드민 전용). */
    fun updateRestricted(value: Boolean) {
        this.restricted = value
    }
}
