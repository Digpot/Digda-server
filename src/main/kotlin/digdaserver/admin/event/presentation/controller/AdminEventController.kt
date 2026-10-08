package digdaserver.admin.event.presentation.controller

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.event.application.service.AdminEventService
import digdaserver.admin.event.presentation.dto.req.GrantCoinRequest
import digdaserver.admin.event.presentation.dto.req.UpdateExpEventRequest
import digdaserver.admin.event.presentation.dto.res.CoinGrantResponse
import digdaserver.domain.event.presentation.dto.res.ExpEventResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 어드민 시즌 이벤트 — 모찌 경험치 배수 설정 + 코인 전체 지급.
 * `/api/admin` 하위라 SecurityConfig 에서 ROLE_ADMIN 으로 보호된다.
 */
@RestController
@RequestMapping("/api/admin/events")
@Tag(name = "Admin - Event", description = "관리자 시즌 이벤트(경험치 배수·코인 지급)")
class AdminEventController(
    private val adminEventService: AdminEventService
) {

    @Operation(summary = "경험치 배수 이벤트 조회")
    @GetMapping("/exp")
    fun getExpEvent(): ResponseEntity<ExpEventResponse> =
        ResponseEntity.ok(adminEventService.getExpEvent())

    @Operation(
        summary = "경험치 배수 이벤트 설정",
        description = "켜면 기간 안에서 일기 작성·퀴즈 정답 경험치에 배수가 곱해진다. 코인은 배수 대상이 아니다."
    )
    @PutMapping("/exp")
    fun updateExpEvent(
        @Valid
        @RequestBody
        request: UpdateExpEventRequest
    ): ResponseEntity<ExpEventResponse> =
        ResponseEntity.ok(adminEventService.updateExpEvent(request))

    @Operation(
        summary = "코인 전체 지급",
        description = "살아있는 모든 그룹의 모찌에 코인을 지급한다(그룹당 amount). 되돌릴 수 없으며 이력이 남는다."
    )
    @PostMapping("/coin-grants")
    fun grantCoin(
        @Valid
        @RequestBody
        request: GrantCoinRequest
    ): ResponseEntity<CoinGrantResponse> =
        ResponseEntity.ok(adminEventService.grantCoinToAll(request))

    @Operation(summary = "코인 지급 이력 조회", description = "최신순")
    @GetMapping("/coin-grants")
    fun searchCoinGrants(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<CoinGrantResponse>> =
        ResponseEntity.ok(adminEventService.searchCoinGrants(page, size))
}
