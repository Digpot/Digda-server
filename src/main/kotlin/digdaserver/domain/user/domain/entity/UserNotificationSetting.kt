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
@Table(name = "user_notification_setting")
class UserNotificationSetting(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_notification_setting_id")
    val id: Long = 0L,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "push_enabled", nullable = false)
    var pushEnabled: Boolean = true,

    @Column(name = "schedule_notification", nullable = false)
    var scheduleNotification: Boolean = true,

    @Column(name = "diary_notification", nullable = false)
    var diaryNotification: Boolean = true,

    @Column(name = "comment_notification", nullable = false)
    var commentNotification: Boolean = true,

    @Column(name = "marketing_consent", nullable = false)
    var marketingConsent: Boolean = false
) {

    fun update(
        pushEnabled: Boolean?,
        scheduleNotification: Boolean?,
        diaryNotification: Boolean?,
        commentNotification: Boolean?,
        marketingConsent: Boolean?
    ) {
        pushEnabled?.let { this.pushEnabled = it }
        scheduleNotification?.let { this.scheduleNotification = it }
        diaryNotification?.let { this.diaryNotification = it }
        commentNotification?.let { this.commentNotification = it }
        marketingConsent?.let { this.marketingConsent = it }
    }
}
