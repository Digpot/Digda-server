package digdaserver.domain.user.application.service

import digdaserver.domain.user.presentation.dto.req.UpdateNotificationSettingRequest
import digdaserver.domain.user.presentation.dto.res.NotificationSettingResponse
import java.util.UUID

interface UserNotificationSettingService {

    fun getNotificationSetting(userId: UUID): NotificationSettingResponse

    fun updateNotificationSetting(userId: UUID, request: UpdateNotificationSettingRequest): NotificationSettingResponse
}
