package digdaserver.domain.device.application.service

import digdaserver.domain.device.presentation.dto.res.RegisterDeviceResponse
import java.util.UUID

interface DeviceService {

    fun registerDevice(userId: UUID, token: String, platform: String): RegisterDeviceResponse

    fun unregisterDevice(userId: UUID, deviceId: Long)
}
