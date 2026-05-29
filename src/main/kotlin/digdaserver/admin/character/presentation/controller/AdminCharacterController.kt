package digdaserver.admin.character.presentation.controller

import digdaserver.admin.character.application.service.AdminCharacterService
import digdaserver.admin.character.presentation.dto.req.AdminUpdateCharacterRequest
import digdaserver.admin.character.presentation.dto.res.AdminCharacterResponse
import digdaserver.admin.common.dto.res.AdminPageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 어드민 모찌(GroupCharacter) 관리.
 *
 * 그룹방 1:1 캐릭터라 검색·상세 모두 `groupRoomId` 기준으로 다룬다 (캐릭터 PK 가 아닌).
 * 어드민 입력값(레벨/코인/디코)은 서비스 계층에서 자동 정합 처리하므로 컨트롤러는 단순 위임.
 */
@RestController
@RequestMapping("/api/admin/characters")
@Tag(name = "Admin - Character", description = "관리자 모찌(그룹 캐릭터) 관리 API")
class AdminCharacterController(
    private val adminCharacterService: AdminCharacterService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "모찌 목록 조회", description = "키워드(그룹방 이름/방장 이름), 삭제 포함 여부, 페이지네이션")
    @GetMapping
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "false") includeDeletedGroups: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminCharacterResponse>> {
        log.info(
            "api=GET /api/admin/characters, keyword={}, includeDeletedGroups={}, page={}",
            keyword,
            includeDeletedGroups,
            page
        )
        return ResponseEntity.ok(
            adminCharacterService.search(keyword, includeDeletedGroups, page, size)
        )
    }

    @Operation(summary = "모찌 상세 조회 (그룹방 ID 기준)")
    @GetMapping("/{groupRoomId}")
    fun getDetail(@PathVariable groupRoomId: Long): ResponseEntity<AdminCharacterResponse> {
        return ResponseEntity.ok(adminCharacterService.getDetail(groupRoomId))
    }

    @Operation(
        summary = "모찌 정보 수정 (레벨/코인/디코)",
        description = "전송된 필드만 수정. 레벨 변경 시 stage·exp 자동 정합 + Lv.10 도달 시 디코 자동 해금."
    )
    @PatchMapping("/{groupRoomId}")
    fun update(
        @PathVariable groupRoomId: Long,
        @Valid
        @RequestBody
        request: AdminUpdateCharacterRequest
    ): ResponseEntity<AdminCharacterResponse> {
        log.info(
            "api=PATCH /api/admin/characters/{}, level={}, coin={}, dikoUnlocked={}",
            groupRoomId,
            request.level,
            request.coin,
            request.dikoUnlocked
        )
        return ResponseEntity.ok(adminCharacterService.update(groupRoomId, request))
    }
}
