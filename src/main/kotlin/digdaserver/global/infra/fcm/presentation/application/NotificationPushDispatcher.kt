package digdaserver.global.infra.fcm.presentation.application

import digdaserver.domain.notification.application.dto.NotificationPayload
import digdaserver.domain.user.domain.entity.User

interface NotificationPushDispatcher {

    fun dispatch(recipients: List<User>, payload: NotificationPayload)
}
