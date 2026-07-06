package digdaserver.domain.user.presentation.dto.req

data class UpdateNotificationSettingRequest(
    val pushEnabled: Boolean? = null,
    val scheduleNotification: Boolean? = null,
    val diaryNotification: Boolean? = null,
    val commentNotification: Boolean? = null,
    val marketingConsent: Boolean? = null
)
