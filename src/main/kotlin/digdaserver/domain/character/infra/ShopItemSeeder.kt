package digdaserver.domain.character.infra

import digdaserver.domain.character.domain.entity.ShopItem
import digdaserver.domain.character.domain.entity.ShopItemType
import digdaserver.domain.character.domain.repository.ShopItemRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 상점 마스터 데이터 idempotent 시드.
 *
 * 운영 DB 는 마이그레이션 SQL 로 이미 적용된다는 가정이지만, 로컬·테스트 환경에서
 * Flyway 같은 도구 없이도 코드 한 곳에서 마스터 데이터를 일관되게 유지하려고 부팅
 * 시 보정 로직을 둔다. [ShopItem.itemKey] 가 키.
 */
@Component
class ShopItemSeeder(
    private val shopItemRepository: ShopItemRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun seed() {
        var created = 0
        var updated = 0
        SeedCatalog.all.forEach { def ->
            val existing = shopItemRepository.findByItemKey(def.itemKey)
            if (existing == null) {
                shopItemRepository.save(
                    ShopItem(
                        itemKey = def.itemKey,
                        itemType = def.itemType,
                        displayName = def.displayName,
                        description = def.description,
                        cost = def.cost,
                        assetKey = def.assetKey,
                        accentColor = def.accentColor,
                        layerOrder = def.layerOrder,
                        sortOrder = def.sortOrder,
                        isDefault = def.isDefault,
                        enabled = true
                    )
                )
                created += 1
            } else {
                existing.updateMeta(
                    displayName = def.displayName,
                    description = def.description,
                    cost = def.cost,
                    assetKey = def.assetKey,
                    accentColor = def.accentColor,
                    layerOrder = def.layerOrder,
                    sortOrder = def.sortOrder,
                    isDefault = def.isDefault,
                    enabled = true
                )
                updated += 1
            }
        }
        log.info("action=shop_item_seed_done, created={}, updated={}", created, updated)
    }

    data class ItemDef(
        val itemKey: String,
        val itemType: ShopItemType,
        val displayName: String,
        val description: String?,
        val cost: Int,
        val assetKey: String,
        val accentColor: String?,
        val layerOrder: Int,
        val sortOrder: Int,
        val isDefault: Boolean
    )

    object SeedCatalog {
        val all: List<ItemDef> = listOf(
            // ─── SKIN ───────────────────────────────────────────────
            ItemDef(
                itemKey = "skin_coral",
                itemType = ShopItemType.SKIN,
                displayName = "코랄 모찌",
                description = "기본 코랄 톤의 모찌",
                cost = 0,
                assetKey = "skin/coral",
                accentColor = "#FF6B6B",
                layerOrder = 0,
                sortOrder = 10,
                isDefault = true
            ),
            ItemDef(
                itemKey = "skin_panda",
                itemType = ShopItemType.SKIN,
                displayName = "판다 모찌",
                description = "흑백 판다 패턴의 특별 스킨",
                cost = 300,
                assetKey = "skin/panda",
                accentColor = "#2F2F2F",
                layerOrder = 0,
                sortOrder = 20,
                isDefault = false
            ),
            // ─── GLASSES ────────────────────────────────────────────
            ItemDef("glasses_round", ShopItemType.GLASSES, "동그란 안경", "클래식한 동글이 안경", 100, "item/glasses_round", null, 50, 10, false),
            ItemDef("glasses_heart", ShopItemType.GLASSES, "하트 선글라스", "하트 모양 선글라스", 150, "item/glasses_heart", null, 50, 20, false),
            // ─── HAIRPIN ────────────────────────────────────────────
            ItemDef("hairpin_star", ShopItemType.HAIRPIN, "별 머리핀", "반짝이는 별 머리핀", 60, "item/hairpin_star", null, 30, 10, false),
            ItemDef("hairpin_ribbon", ShopItemType.HAIRPIN, "리본 머리핀", "귀여운 리본 머리핀", 80, "item/hairpin_ribbon", null, 30, 20, false),
            // ─── HAT ────────────────────────────────────────────────
            ItemDef("hat_party", ShopItemType.HAT, "파티 모자", "신나는 파티 모자", 80, "item/hat_party", null, 60, 10, false),
            ItemDef("hat_chef", ShopItemType.HAT, "요리사 모자", "하얀 요리사 모자", 150, "item/hat_chef", null, 60, 20, false),
            // ─── ACCESSORY ──────────────────────────────────────────
            ItemDef("accessory_bowtie", ShopItemType.ACCESSORY, "보타이", "깔끔한 보타이", 100, "item/bowtie", null, 20, 10, false),
            ItemDef("accessory_scarf", ShopItemType.ACCESSORY, "목도리", "따뜻한 목도리", 120, "item/scarf", null, 20, 20, false),
            // ─── MISC ───────────────────────────────────────────────
            ItemDef("misc_balloon", ShopItemType.MISC, "풍선", "함께 떠다니는 풍선", 80, "item/balloon", null, 5, 10, false),
            ItemDef("misc_flower", ShopItemType.MISC, "꽃 한송이", "곁에 둔 꽃 한송이", 60, "item/flower", null, 5, 20, false)
        )
    }
}
