package digdaserver.admin.announcement.presentation.controller

import digdaserver.admin.announcement.presentation.dto.req.SendAnnouncementRequest
import digdaserver.admin.announcement.presentation.dto.res.SendAnnouncementResponse
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/announcements")
@Tag(name = "Admin - Announcement", description = "관리자 공지 발송 API")
class AdminAnnouncementController(
    private val notificationService: NotificationService
) {

    @Operation(
        summary = "공지 발송",
        description = "ALL = 전체 사용자에게 발송, USER_IDS = 지정 유저들에게 발송. 알림 레코드 생성 + FCM 푸시 동시 발송."
    )
    @PostMapping
    fun send(@Valid @RequestBody request: SendAnnouncementRequest): ResponseEntity<SendAnnouncementResponse> {
        val targetUserIds = when (request.target.uppercase()) {
            "ALL" -> null
            "USER_IDS" -> request.userIds?.takeIf { it.isNotEmpty() }
                ?: throw DigdaException(ErrorCode.INVALID_PARAMETER, "USER_IDS 발송 시 userIds가 비어있을 수 없습니다")
            else -> throw DigdaException(ErrorCode.INVALID_PARAMETER, "target은 ALL 또는 USER_IDS 만 허용됩니다")
        }

        val count = notificationService.sendAnnouncement(targetUserIds, request.title, request.body)
        return ResponseEntity.ok(SendAnnouncementResponse(recipientCount = count))
    }
}
