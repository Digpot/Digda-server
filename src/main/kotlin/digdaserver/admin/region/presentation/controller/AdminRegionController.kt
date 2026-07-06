package digdaserver.admin.region.presentation.controller

import digdaserver.admin.region.application.service.AdminRegionService
import digdaserver.admin.region.presentation.dto.req.RegionFillRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 어드민 시그니처 지도 채움 관리 — 그룹의 시·군·구를 임의로 채우거나 해제한다.
 * 채운 지역은 region-map 응답에 병합되어 앱 지도에서 색칠된다.
 * `/api/admin` 하위라 SecurityConfig 에서 ROLE_ADMIN 으로 보호된다.
 */
@RestController
@RequestMapping("/api/admin/region-map")
@Tag(name = "Admin - Region Map", description = "관리자 시그니처 지도 채움 관리 API")
class AdminRegionController(
    private val adminRegionService: AdminRegionService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "그룹의 채운 지역 목록")
    @GetMapping
    fun filled(@RequestParam groupRoomId: Long): ResponseEntity<List<String>> =
        ResponseEntity.ok(adminRegionService.filled(groupRoomId))

    @Operation(summary = "지역 채우기", description = "region_key 목록을 채움(멱등). '전체 채우기'는 프론트가 전 지역 키를 보낸다.")
    @PostMapping("/fill")
    fun fill(@RequestBody request: RegionFillRequest): ResponseEntity<List<String>> {
        log.info("api=POST /api/admin/region-map/fill, groupRoomId={}, count={}", request.groupRoomId, request.regionKeys.size)
        return ResponseEntity.ok(adminRegionService.fill(request.groupRoomId, request.regionKeys))
    }

    @Operation(summary = "지역 채움 해제", description = "지정 region_key 들의 채움 해제")
    @PostMapping("/unfill")
    fun unfill(@RequestBody request: RegionFillRequest): ResponseEntity<List<String>> {
        log.info("api=POST /api/admin/region-map/unfill, groupRoomId={}, count={}", request.groupRoomId, request.regionKeys.size)
        return ResponseEntity.ok(adminRegionService.unfill(request.groupRoomId, request.regionKeys))
    }

    @Operation(summary = "그룹 채움 전체 해제")
    @DeleteMapping
    fun clear(@RequestParam groupRoomId: Long): ResponseEntity<Void> {
        adminRegionService.clear(groupRoomId)
        return ResponseEntity.noContent().build()
    }
}
