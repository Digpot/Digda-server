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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/character")
@Tag(name = "Character", description = "캐릭터(모찌) 키우기 API — 그룹 1개당 1마리 공유")
class CharacterController(
    private val characterService: CharacterService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(
        summary = "그룹 캐릭터 상태 조회",
        description = "해당 그룹의 모찌. 첫 진입 시 자동 생성됩니다. 호출자가 그룹 멤버여야 함."
    )
    @GetMapping
    fun getGroupCharacter(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long
    ): ResponseEntity<CharacterStateResponse> {
        log.info("api=GET /character, userId={}, groupRoomId={}", userId, groupRoomId)
        return ResponseEntity.ok(
            characterService.getGroupCharacter(UUID.fromString(userId), groupRoomId)
        )
    }

    @Operation(
        summary = "경험치 가산",
        description = "그룹 캐릭터에 amount만큼 EXP를 더하고, 레벨업/진화는 서버가 계산해 응답에 포함합니다."
    )
    @PostMapping("/exp")
    fun gainExp(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestBody request: AddExpRequest
    ): ResponseEntity<AddExpResponse> {
        log.info(
            "api=POST /character/exp, userId={}, groupRoomId={}, amount={}, source={}",
            userId,
            groupRoomId,
            request.amount,
            request.source
        )
        return ResponseEntity.ok(
            characterService.gainExp(
                userId = UUID.fromString(userId),
                groupRoomId = groupRoomId,
                amount = request.amount,
                coinDelta = 0,
                source = request.source
            )
        )
    }

    @Operation(summary = "진화 트리 조회", description = "전체 단계 + 그룹 캐릭터 도달 여부.")
    @GetMapping("/stages")
    fun getStageTree(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long
    ): ResponseEntity<CharacterStageTreeResponse> {
        log.info("api=GET /character/stages, userId={}, groupRoomId={}", userId, groupRoomId)
        return ResponseEntity.ok(
            characterService.getStageTree(UUID.fromString(userId), groupRoomId)
        )
    }

    @Operation(
        summary = "색상 상점 조회",
        description = "구매 가능 색상 + 그룹 보유/현재 여부 + 잔액 코인."
    )
    @GetMapping("/shop/colors")
    fun getColorShop(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long
    ): ResponseEntity<CharacterColorShopResponse> {
        log.info("api=GET /character/shop/colors, userId={}, groupRoomId={}", userId, groupRoomId)
        return ResponseEntity.ok(
            characterService.getColorShop(UUID.fromString(userId), groupRoomId)
        )
    }

    @Operation(
        summary = "색상 구매",
        description = "그룹 코인을 차감해 색상을 영구 해금합니다. 그룹원 누구나 호출 가능."
    )
    @PostMapping("/shop/colors/{color}/buy")
    fun buyColor(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @PathVariable color: CharacterColor
    ): ResponseEntity<CharacterColorShopResponse> {
        log.info(
            "api=POST /character/shop/colors/{}/buy, userId={}, groupRoomId={}",
            color,
            userId,
            groupRoomId
        )
        return ResponseEntity.ok(
            characterService.buyColor(UUID.fromString(userId), groupRoomId, color)
        )
    }

    @Operation(
        summary = "색상 변경 적용",
        description = "보유한 색으로 그룹 캐릭터 색을 변경합니다. 그룹원 누구나 호출 가능."
    )
    @PutMapping("/color")
    fun applyColor(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestBody request: ChangeColorRequest
    ): ResponseEntity<CharacterStateResponse> {
        log.info(
            "api=PUT /character/color, userId={}, groupRoomId={}, color={}",
            userId,
            groupRoomId,
            request.color
        )
        return ResponseEntity.ok(
            characterService.applyColor(UUID.fromString(userId), groupRoomId, request.color)
        )
    }
}
