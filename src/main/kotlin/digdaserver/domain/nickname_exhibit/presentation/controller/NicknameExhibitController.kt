package digdaserver.domain.nickname_exhibit.presentation.controller

import digdaserver.domain.nickname_exhibit.application.service.NicknameExhibitService
import digdaserver.domain.nickname_exhibit.presentation.dto.res.NicknameExhibitAccessResponse
import digdaserver.domain.nickname_exhibit.presentation.dto.res.NicknameExhibitResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/nickname-exhibits")
@Tag(name = "Nickname Exhibit", description = "디그다 역대 별명 전시관 API (재미용 콘텐츠)")
class NicknameExhibitController(
    private val nicknameExhibitService: NicknameExhibitService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(
        summary = "전시관 접근 권한 조회",
        description = "현재 사용자가 전시관에 접근 가능한지(어드민이 허용했는지) 반환. 앱은 이 값으로 버튼 노출을 결정."
    )
    @GetMapping("/access")
    fun checkAccess(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<NicknameExhibitAccessResponse> {
        val allowed = nicknameExhibitService.hasAccess(UUID.fromString(userId))
        return ResponseEntity.ok(NicknameExhibitAccessResponse(allowed = allowed))
    }

    @Operation(
        summary = "전시관 별명 카드 목록 조회",
        description = "접근 허용 사용자만 조회 가능. 미허용 시 403(EXHIBIT_ACCESS_DENIED)."
    )
    @GetMapping
    fun list(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<List<NicknameExhibitResponse>> {
        log.info("api=GET /nickname-exhibits, userId={}", userId)
        return ResponseEntity.ok(nicknameExhibitService.list(UUID.fromString(userId)))
    }
}
