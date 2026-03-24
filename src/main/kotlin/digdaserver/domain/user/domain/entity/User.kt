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
    name = "users",
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

    @Column(nullable = false, length = 20)
    var name: String,

    @Column(name = "profile_image")
    var profileImage: String? = null,

    @Column(name = "status_message", length = 100)
    var statusMessage: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false)
    val socialProvider: SocialProvider,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER

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

    fun updateProfile(name: String?, statusMessage: String?, profileImage: String?) {
        name?.let { this.name = it }
        statusMessage?.let { this.statusMessage = it }
        profileImage?.let { this.profileImage = it }
    }

    fun resetProfileImage() {
        this.profileImage = null
    }

    fun clearStatusMessage() {
        this.statusMessage = null
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
}
