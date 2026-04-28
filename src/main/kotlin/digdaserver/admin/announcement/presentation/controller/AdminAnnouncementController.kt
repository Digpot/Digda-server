package digdaserver.admin.announcement.presentation.controller

import digdaserver.admin.announcement.presentation.dto.req.SendAnnouncementRequest
import digdaserver.admin.announcement.presentation.dto.res.AdminAnnouncementResponse
import digdaserver.admin.announcement.presentation.dto.res.SendAnnouncementResponse
import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.domain.announcement.application.service.AnnouncementService
import digdaserver.domain.announcement.domain.entity.AnnouncementTarget
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/announcements")
@Tag(name = "Admin - Announcement", description = "관리자 공지 발송/조회 API")
class AdminAnnouncementController(
    private val announcementService: AnnouncementService
) {

    @Operation(
        summary = "공지 발송",
        description = "ALL = 전체 사용자에게 발송, USER_IDS = 지정 유저들에게 발송. 알림 레코드 생성 + FCM 푸시 동시 발송."
    )
    @PostMapping
    fun send(@Valid @RequestBody request: SendAnnouncementRequest): ResponseEntity<SendAnnouncementResponse> {
        val target = parseTarget(request.target)
        val targetUserIds = when (target) {
            AnnouncementTarget.ALL -> null
            AnnouncementTarget.USER_IDS -> request.userIds?.takeIf { it.isNotEmpty() }
                ?: throw DigdaException(ErrorCode.INVALID_PARAMETER, "USER_IDS 발송 시 userIds가 비어있을 수 없습니다")
        }

        val saved = announcementService.send(target, targetUserIds, request.title, request.body)
        return ResponseEntity.ok(SendAnnouncementResponse(recipientCount = saved.recipientCount))
    }

    @Operation(
        summary = "공지 목록 조회",
        description = "최신순 정렬. 제목/본문 키워드 검색 지원."
    )
    @GetMapping
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminAnnouncementResponse>> {
        val result = announcementService.search(keyword, page, size)
        return ResponseEntity.ok(AdminPageResponse.of(result, AdminAnnouncementResponse::from))
    }

    private fun parseTarget(raw: String): AnnouncementTarget =
        when (raw.uppercase()) {
            "ALL" -> AnnouncementTarget.ALL
            "USER_IDS" -> AnnouncementTarget.USER_IDS
            else -> throw DigdaException(ErrorCode.INVALID_PARAMETER, "target은 ALL 또는 USER_IDS 만 허용됩니다")
        }
}
