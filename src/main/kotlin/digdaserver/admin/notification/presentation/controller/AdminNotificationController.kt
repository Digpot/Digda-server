package digdaserver.admin.notification.presentation.controller

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.notification.application.service.AdminNotificationService
import digdaserver.admin.notification.presentation.dto.res.AdminNotificationResponse
import digdaserver.domain.notification.domain.entity.NotificationType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 어드민용 알림 조회. 사용자별 알림 row 를 그대로 노출하므로 동일 이벤트가 그룹원 N명에게
 * 전달된 경우 N개 row 가 보인다 (raw 데이터 우선).
 *
 * 모찌 관련만 추리려면 type 필터로 `MOCHI_LEVELUP/DIKO_UNLOCKED/QUIZ_CREATED/QUIZ_ANSWERED`
 * 를 함께 전달.
 */
@RestController
@RequestMapping("/api/admin/notifications")
@Tag(name = "Admin - Notification", description = "관리자 알림 조회 API")
class AdminNotificationController(
    private val adminNotificationService: AdminNotificationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(
        summary = "알림 목록 조회",
        description = "type(쉼표 구분 문자열, 예: MOCHI_LEVELUP,DIKO_UNLOCKED), groupRoomId, keyword 필터 + 페이지네이션"
    )
    @GetMapping
    fun search(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) groupRoomId: Long?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminNotificationResponse>> {
        val types: Set<NotificationType>? = type
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull {
                runCatching { NotificationType.valueOf(it.uppercase()) }.getOrNull()
            }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
        log.info(
            "api=GET /api/admin/notifications, types={}, groupRoomId={}, keyword={}, page={}",
            types,
            groupRoomId,
            keyword,
            page
        )
        return ResponseEntity.ok(
            adminNotificationService.search(types, groupRoomId, keyword, page, size)
        )
    }
}
