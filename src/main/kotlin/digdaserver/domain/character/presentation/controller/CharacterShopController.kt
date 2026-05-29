package digdaserver.domain.character.presentation.controller

import digdaserver.domain.character.application.service.CharacterShopService
import digdaserver.domain.character.domain.entity.ShopItemType
import digdaserver.domain.character.presentation.dto.req.EquipItemRequest
import digdaserver.domain.character.presentation.dto.res.CharacterShopResponse
import digdaserver.domain.character.presentation.dto.res.CharacterStateResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
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
@RequestMapping("/character/shop")
@Tag(name = "CharacterShop", description = "캐릭터 아이템 상점 — 카테고리별 구매·장착 관리")
class CharacterShopController(
    private val shopService: CharacterShopService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "상점 조회", description = "카테고리별 아이템 목록 + 보유/장착 여부 + 잔액.")
    @GetMapping
    fun getShop(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long
    ): ResponseEntity<CharacterShopResponse> {
        log.info("api=GET /character/shop, userId={}, groupRoomId={}", userId, groupRoomId)
        return ResponseEntity.ok(
            shopService.getShop(UUID.fromString(userId), groupRoomId)
        )
    }

    @Operation(
        summary = "아이템 구매",
        description = "그룹 코인을 차감해 아이템을 영구 보유합니다. 보유 후 자동 장착은 하지 않음."
    )
    @PostMapping("/items/{itemKey}/buy")
    fun buyItem(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @PathVariable itemKey: String
    ): ResponseEntity<CharacterShopResponse> {
        log.info(
            "api=POST /character/shop/items/{}/buy, userId={}, groupRoomId={}",
            itemKey,
            userId,
            groupRoomId
        )
        return ResponseEntity.ok(
            shopService.buyItem(UUID.fromString(userId), groupRoomId, itemKey)
        )
    }

    @Operation(
        summary = "아이템 장착",
        description = "보유한 아이템을 해당 카테고리 슬롯에 장착. 같은 카테고리의 이전 장착은 자동 교체."
    )
    @PutMapping("/equip")
    fun equipItem(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestBody request: EquipItemRequest
    ): ResponseEntity<CharacterStateResponse> {
        log.info(
            "api=PUT /character/shop/equip, userId={}, groupRoomId={}, itemKey={}",
            userId,
            groupRoomId,
            request.itemKey
        )
        return ResponseEntity.ok(
            shopService.equipItem(UUID.fromString(userId), groupRoomId, request.itemKey)
        )
    }

    @Operation(
        summary = "카테고리 슬롯 해제",
        description = "지정 카테고리의 장착을 해제. SKIN 은 default 로 복귀, 그 외는 빈 슬롯."
    )
    @DeleteMapping("/equip/{itemType}")
    fun unequipSlot(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @PathVariable itemType: String
    ): ResponseEntity<CharacterStateResponse> {
        log.info(
            "api=DELETE /character/shop/equip/{}, userId={}, groupRoomId={}",
            itemType,
            userId,
            groupRoomId
        )
        val type = ShopItemType.safeValueOf(itemType.uppercase())
            ?: throw DigdaException(ErrorCode.INVALID_PARAMETER)
        return ResponseEntity.ok(
            shopService.unequipSlot(UUID.fromString(userId), groupRoomId, type)
        )
    }
}
