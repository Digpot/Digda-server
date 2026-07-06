package digdaserver.domain.title.presentation.controller

import digdaserver.domain.title.application.service.TitleService
import digdaserver.domain.title.presentation.dto.req.ClaimTitlesRequest
import digdaserver.domain.title.presentation.dto.req.EquipTitleRequest
import digdaserver.domain.title.presentation.dto.res.EquippedTitleResponse
import digdaserver.domain.title.presentation.dto.res.TitleCatalogResponse
import digdaserver.domain.title.presentation.dto.res.TitleResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/titles")
@Tag(name = "Title", description = "칭호 API — 계정 단위로 보관되며 그룹방 탈퇴/삭제와 무관하게 유지된다")
class TitleController(
    private val titleService: TitleService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(
        summary = "칭호 카탈로그 조회",
        description = "전체 칭호 정의(이름/색/아이콘/조건). 앱이 code 로 매핑해 렌더한다."
    )
    @GetMapping("/catalog")
    fun catalog(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<List<TitleCatalogResponse>> {
        return ResponseEntity.ok(titleService.catalog())
    }

    @Operation(
        summary = "내 칭호 목록 조회",
        description = "획득한 칭호 전체를 반환. 작성 일기 수 칭호는 조회 시점에 자동 적재된다. 표시 메타는 앱이 code 로 매핑."
    )
    @GetMapping
    fun list(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<List<TitleResponse>> {
        return ResponseEntity.ok(titleService.list(UUID.fromString(userId)))
    }

    @Operation(
        summary = "칭호 획득 적재(멱등)",
        description = "앱이 판정한 지역 정복·캐릭터 칭호 등을 적재. 이미 보유/비멤버 그룹/잘못된 코드는 무시. 적재 후 전체 목록 반환."
    )
    @PostMapping("/claim")
    fun claim(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: ClaimTitlesRequest
    ): ResponseEntity<List<TitleResponse>> {
        log.info("api=POST /titles/claim, userId={}, count={}", userId, request.titles.size)
        return ResponseEntity.ok(titleService.claim(UUID.fromString(userId), request.titles))
    }

    @Operation(
        summary = "그룹 모찌 장착 칭호 조회",
        description = "그룹 모찌에 장착된 칭호(code). 미장착이면 code=null."
    )
    @GetMapping("/equipped")
    fun equipped(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long
    ): ResponseEntity<EquippedTitleResponse> {
        return ResponseEntity.ok(titleService.equippedTitle(groupRoomId))
    }

    @Operation(
        summary = "그룹 모찌에 칭호 장착/해제",
        description = "본인이 획득한 칭호만 장착 가능. code=null 이면 해제. 그룹 구성원만 호출 가능."
    )
    @PutMapping("/equip")
    fun equip(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: EquipTitleRequest
    ): ResponseEntity<EquippedTitleResponse> {
        log.info(
            "api=PUT /titles/equip, userId={}, groupRoomId={}, code={}",
            userId,
            request.groupRoomId,
            request.code
        )
        return ResponseEntity.ok(
            titleService.equipTitle(UUID.fromString(userId), request.groupRoomId, request.code)
        )
    }
}
