package digdaserver.domain.character.presentation.controller

import digdaserver.domain.character.application.service.CharacterService
import digdaserver.domain.character.domain.entity.CharacterColor
import digdaserver.domain.character.presentation.dto.req.AddExpRequest
import digdaserver.domain.character.presentation.dto.req.ChangeColorRequest
import digdaserver.domain.character.presentation.dto.res.AddExpResponse
import digdaserver.domain.character.presentation.dto.res.CharacterColorShopResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStageTreeResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStateResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/character")
@Tag(name = "Character", description = "캐릭터(모찌) 키우기 API")
class CharacterController(
    private val characterService: CharacterService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "내 캐릭터 상태 조회", description = "첫 진입 시 자동 생성됩니다.")
    @GetMapping("/me")
    fun getMyCharacter(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<CharacterStateResponse> {
        log.info("api=GET /character/me, userId={}", userId)
        return ResponseEntity.ok(characterService.getMyCharacter(UUID.fromString(userId)))
    }

    @Operation(summary = "경험치 가산", description = "amount만큼 EXP를 더하고, 레벨업/진화는 서버가 계산해 응답에 포함합니다.")
    @PostMapping("/me/exp")
    fun gainExp(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: AddExpRequest
    ): ResponseEntity<AddExpResponse> {
        log.info("api=POST /character/me/exp, userId={}, amount={}, source={}", userId, request.amount, request.source)
        return ResponseEntity.ok(
            characterService.gainExp(
                userId = UUID.fromString(userId),
                amount = request.amount,
                coinDelta = 0,
                source = request.source
            )
        )
    }

    @Operation(summary = "진화 트리 조회", description = "전체 단계 + 내 도달 여부.")
    @GetMapping("/stages")
    fun getStageTree(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<CharacterStageTreeResponse> {
        log.info("api=GET /character/stages, userId={}", userId)
        return ResponseEntity.ok(characterService.getStageTree(UUID.fromString(userId)))
    }

    @Operation(summary = "색상 상점 조회", description = "구매 가능 색상 + 내 보유/현재 여부 + 잔액 코인.")
    @GetMapping("/shop/colors")
    fun getColorShop(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<CharacterColorShopResponse> {
        log.info("api=GET /character/shop/colors, userId={}", userId)
        return ResponseEntity.ok(characterService.getColorShop(UUID.fromString(userId)))
    }

    @Operation(summary = "색상 구매", description = "코인을 차감해 색상을 영구 해금합니다.")
    @PostMapping("/shop/colors/{color}/buy")
    fun buyColor(
        @AuthenticationPrincipal userId: String,
        @PathVariable color: CharacterColor
    ): ResponseEntity<CharacterColorShopResponse> {
        log.info("api=POST /character/shop/colors/{}/buy, userId={}", color, userId)
        return ResponseEntity.ok(characterService.buyColor(UUID.fromString(userId), color))
    }

    @Operation(summary = "색상 변경 적용", description = "보유한 색으로 현재 캐릭터 색을 변경합니다.")
    @PutMapping("/me/color")
    fun applyColor(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: ChangeColorRequest
    ): ResponseEntity<CharacterStateResponse> {
        log.info("api=PUT /character/me/color, userId={}, color={}", userId, request.color)
        return ResponseEntity.ok(characterService.applyColor(UUID.fromString(userId), request.color))
    }
}
