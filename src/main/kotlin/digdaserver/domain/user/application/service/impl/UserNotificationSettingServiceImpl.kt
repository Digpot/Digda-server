package digdaserver.domain.user.application.service.impl

import digdaserver.domain.user.application.service.UserNotificationSettingService
import digdaserver.domain.user.domain.repository.UserNotificationSettingRepository
import digdaserver.domain.user.presentation.dto.req.UpdateNotificationSettingRequest
import digdaserver.domain.user.presentation.dto.res.NotificationSettingResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserNotificationSettingServiceImpl(
    private val userNotificationSettingRepository: UserNotificationSettingRepository
) : UserNotificationSettingService {

    override fun getNotificationSetting(userId: UUID): NotificationSettingResponse {
        val setting = userNotificationSettingRepository.findByUserId(userId)
            .orElseThrow { DigdaException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND) }

        return NotificationSettingResponse.from(setting)
    }

    @Transactional
    override fun updateNotificationSetting(userId: UUID, request: UpdateNotificationSettingRequest): NotificationSettingResponse {
        val setting = userNotificationSettingRepository.findByUserId(userId)
            .orElseThrow { DigdaException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND) }

        setting.update(
            pushEnabled = request.pushEnabled,
            scheduleNotification = request.scheduleNotification,
            diaryNotification = request.diaryNotification,
            commentNotification = request.commentNotification,
            marketingConsent = request.marketingConsent
        )

        return NotificationSettingResponse.from(setting)
    }
}
