package digdaserver.domain.user.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "user_privacy_settings")
class UserPrivacySetting(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_privacy_setting_id")
    val id: Long = 0L,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "profile_public", nullable = false)
    var profilePublic: Boolean = true,

    @Column(name = "activity_visible", nullable = false)
    var activityVisible: Boolean = true
) {

    fun update(profilePublic: Boolean?, activityVisible: Boolean?) {
        profilePublic?.let { this.profilePublic = it }
        activityVisible?.let { this.activityVisible = it }
    }
}
