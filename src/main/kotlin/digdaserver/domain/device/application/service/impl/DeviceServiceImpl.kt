package digdaserver.domain.device.application.service.impl

import digdaserver.domain.device.application.service.DeviceService
import digdaserver.domain.device.domain.entity.Device
import digdaserver.domain.device.domain.entity.Platform
import digdaserver.domain.device.domain.repository.DeviceRepository
import digdaserver.domain.device.presentation.dto.res.RegisterDeviceResponse
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DeviceServiceImpl(
    private val deviceRepository: DeviceRepository,
    private val userRepository: UserRepository
) : DeviceService {

    @Transactional
    override fun registerDevice(userId: UUID, token: String, platform: String): RegisterDeviceResponse {
        if (token.isBlank()) throw DigdaException(ErrorCode.INVALID_PARAMETER)
        val parsedPlatform = parsePlatform(platform)

        val existing = deviceRepository.findByToken(token)
        if (existing.isPresent) {
            val device = existing.get()
            if (device.user.id == userId) {
                // Same user re-registering the same token — idempotent
                return RegisterDeviceResponse(deviceId = device.id)
            }
            // Token belongs to a different user (device hand-off) — release and re-register
            deviceRepository.delete(device)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val saved = deviceRepository.save(
            Device(
                user = user,
                token = token,
                platform = parsedPlatform
            )
        )

        return RegisterDeviceResponse(deviceId = saved.id)
    }

    @Transactional
    override fun unregisterDevice(userId: UUID, deviceId: Long) {
        val device = deviceRepository.findById(deviceId)
            .orElseThrow { DigdaException(ErrorCode.DEVICE_NOT_FOUND) }

        if (device.user.id != userId) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        deviceRepository.delete(device)
    }

    private fun parsePlatform(platform: String): Platform {
        return when (platform.lowercase()) {
            "ios" -> Platform.IOS
            "android" -> Platform.ANDROID
            else -> throw DigdaException(ErrorCode.INVALID_PARAMETER)
        }
    }
}
