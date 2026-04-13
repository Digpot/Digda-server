package digdaserver.domain.user.presentation.dto.res

import digdaserver.domain.user.domain.entity.UserNotificationSetting

data class NotificationSettingResponse(
    val pushEnabled: Boolean,
    val scheduleNotification: Boolean,
    val diaryNotification: Boolean,
    val commentNotification: Boolean,
    val marketingConsent: Boolean
) {
    companion object {
        fun from(setting: UserNotificationSetting): NotificationSettingResponse = NotificationSettingResponse(
            pushEnabled = setting.pushEnabled,
            scheduleNotification = setting.scheduleNotification,
            diaryNotification = setting.diaryNotification,
            commentNotification = setting.commentNotification,
            marketingConsent = setting.marketingConsent
        )
    }
}
