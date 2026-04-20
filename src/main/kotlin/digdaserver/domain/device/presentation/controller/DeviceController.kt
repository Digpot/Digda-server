package digdaserver.domain.device.presentation.controller

import digdaserver.domain.device.application.service.DeviceService
import digdaserver.domain.device.presentation.dto.req.RegisterDeviceRequest
import digdaserver.domain.device.presentation.dto.res.RegisterDeviceResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Tag(name = "Device", description = "디바이스(FCM) API")
class DeviceController(
    private val deviceService: DeviceService
) {

    @Operation(summary = "디바이스 토큰 등록", description = "앱 시작 시 또는 토큰 갱신 시 FCM 토큰을 등록합니다. 동일 토큰이면 upsert.")
    @PostMapping("/devices")
    fun registerDevice(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: RegisterDeviceRequest
    ): ResponseEntity<RegisterDeviceResponse> {
        val response = deviceService.registerDevice(UUID.fromString(userId), request.token, request.platform)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "디바이스 토큰 해제", description = "로그아웃 시 또는 토큰 만료 시 디바이스를 해제합니다.")
    @DeleteMapping("/devices/{deviceId}")
    fun unregisterDevice(
        @AuthenticationPrincipal userId: String,
        @PathVariable deviceId: Long
    ): ResponseEntity<Void> {
        deviceService.unregisterDevice(UUID.fromString(userId), deviceId)
        return ResponseEntity.noContent().build()
    }
}
