package digdaserver.domain.device.application.service.impl

import digdaserver.domain.device.application.service.DeviceService
import digdaserver.domain.device.domain.entity.Device
import digdaserver.domain.device.domain.entity.Platform
import digdaserver.domain.device.domain.repository.DeviceRepository
import digdaserver.domain.device.presentation.dto.res.RegisterDeviceResponse
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DeviceServiceImpl(
    private val deviceRepository: DeviceRepository,
    private val userRepository: UserRepository
) : DeviceService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun registerDevice(userId: UUID, token: String, platform: String): RegisterDeviceResponse {
        // iOS FCM 미수신 디버깅: 어떤 플랫폼 등록 요청이 실제로 도착했는지 추적한다.
        log.info("action=device_register_요청, userId={}, platform={}", userId, platform)
        if (token.isBlank()) {
            log.warn("action=device_register_거부(빈 토큰), userId={}, platform={}", userId, platform)
            throw DigdaException(ErrorCode.INVALID_PARAMETER)
        }
        val parsedPlatform = parsePlatform(platform)

        val existing = deviceRepository.findByToken(token)
        if (existing.isPresent) {
            val device = existing.get()
            if (device.user.id == userId) {
                // Same user re-registering the same token — idempotent
                log.info(
                    "action=device_register_멱등(동일 사용자 동일 토큰), userId={}, platform={}, deviceId={}",
                    userId,
                    parsedPlatform,
                    device.id
                )
                return RegisterDeviceResponse(deviceId = device.id)
            }
            // Token belongs to a different user (device hand-off) — release and re-register
            log.info("action=device_register_토큰 이관(이전 소유자 해제), platform={}, deviceId={}", parsedPlatform, device.id)
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

        log.info("action=device_register_완료, userId={}, platform={}, deviceId={}", userId, parsedPlatform, saved.id)
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
            else -> {
                log.warn("action=device_register_거부(알 수 없는 platform), platform={}", platform)
                throw DigdaException(ErrorCode.INVALID_PARAMETER)
            }
        }
    }
}
