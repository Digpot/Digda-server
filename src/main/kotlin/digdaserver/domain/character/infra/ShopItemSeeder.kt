package digdaserver.domain.character.infra

import digdaserver.domain.character.domain.entity.ShopItem
import digdaserver.domain.character.domain.entity.ShopItemType
import digdaserver.domain.character.domain.repository.GroupCharacterEquippedRepository
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
    private val shopItemRepository: ShopItemRepository,
    private val groupCharacterEquippedRepository: GroupCharacterEquippedRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun seed() {
        retireRemovedItems()
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
                // enabled 는 어드민이 토글할 수 있는 운영 플래그라 시드가 덮어쓰지 않는다.
                existing.updateMeta(
                    displayName = def.displayName,
                    description = def.description,
                    cost = def.cost,
                    assetKey = def.assetKey,
                    accentColor = def.accentColor,
                    layerOrder = def.layerOrder,
                    sortOrder = def.sortOrder,
                    isDefault = def.isDefault,
                    enabled = existing.enabled
                )
                updated += 1
            }
        }
        log.info("action=shop_item_seed_done, created={}, updated={}", created, updated)
    }

    /**
     * 카탈로그에서 뺀 아이템 정리 — 행 삭제 대신 enabled=false 로 상점/장착 경로만 차단하고,
     * 이미 장착 중인 그룹은 같은 타입의 default 아이템으로 되돌린다. (구매 이력 행은 보존)
     */
    private fun retireRemovedItems() {
        SeedCatalog.retiredKeys.forEach { key ->
            val item = shopItemRepository.findByItemKey(key) ?: return@forEach
            if (!item.enabled) return@forEach
            item.enabled = false
            val default = shopItemRepository.findFirstDefaultByItemType(item.itemType)
            var reverted = 0
            if (default != null && default.id != item.id) {
                groupCharacterEquippedRepository.findAllByShopItemId(item.id).forEach { equipped ->
                    equipped.replaceWith(default)
                    reverted += 1
                }
            }
            log.info("action=shop_item_retired, itemKey={}, revertedEquips={}", key, reverted)
        }
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
        /** 판매 종료 아이템 — 부팅 시 enabled=false 처리 + 장착 그룹은 default 복귀. */
        val retiredKeys: Set<String> = setOf("skin_mint", "skin_lavender", "skin_sky")

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
            // 두더지 스킨 — 디그팟 상징 캐릭터. 앱 렌더러(skin/mole 패치)와 assetKey 동기 필수.
            ItemDef(
                itemKey = "skin_mole",
                itemType = ShopItemType.SKIN,
                displayName = "두더지 모찌",
                description = "디그팟의 상징! 땅파기 장인 두더지 모찌",
                cost = 350,
                assetKey = "skin/mole",
                accentColor = "#8B6547",
                layerOrder = 0,
                sortOrder = 30,
                isDefault = false
            ),
            // 캐릭터 패턴 스킨 4종 — 앱 렌더러(mochi_character_view._skinPatches 등)와
            // assetKey 동기 필수. 판다처럼 바디 톤/무늬/귀 장식이 함께 바뀐다.
            ItemDef(
                itemKey = "skin_tiger",
                itemType = ShopItemType.SKIN,
                displayName = "호랑이 모찌",
                description = "어흥! 줄무늬가 늠름한 호랑이 모찌",
                cost = 350,
                assetKey = "skin/tiger",
                accentColor = "#F59E0B",
                layerOrder = 0,
                sortOrder = 60,
                isDefault = false
            ),
            ItemDef(
                itemKey = "skin_cat",
                itemType = ShopItemType.SKIN,
                displayName = "고양이 모찌",
                description = "쫑긋 귀와 수염이 매력적인 고양이 모찌",
                cost = 300,
                assetKey = "skin/cat",
                accentColor = "#B0A8A2",
                layerOrder = 0,
                sortOrder = 70,
                isDefault = false
            ),
            ItemDef(
                itemKey = "skin_bee",
                itemType = ShopItemType.SKIN,
                displayName = "꿀벌 모찌",
                description = "붕붕~ 부지런히 꿀 모으는 꿀벌 모찌",
                cost = 300,
                assetKey = "skin/bee",
                accentColor = "#FCD34D",
                layerOrder = 0,
                sortOrder = 80,
                isDefault = false
            ),
            ItemDef(
                itemKey = "skin_frog",
                itemType = ShopItemType.SKIN,
                displayName = "개구리 모찌",
                description = "개굴개굴 연못에서 온 개구리 모찌",
                cost = 300,
                assetKey = "skin/frog",
                accentColor = "#4ADE80",
                layerOrder = 0,
                sortOrder = 90,
                isDefault = false
            ),
            // ─── BACKGROUND ─────────────────────────────────────────
            // 배경 씬은 렌더 필수 슬롯 — 풀밭 언덕이 default 로 항상 장착된다.
            // layerOrder 는 렌더러가 배경을 별도 레이어로 그려 실사용되지 않지만,
            // "가장 아래" 의미로 음수를 준다.
            ItemDef("bg_meadow", ShopItemType.BACKGROUND, "풀밭 언덕", "스킨 색 따라 물드는 기본 풍경", 0, "bg/meadow", null, -10, 10, true),
            ItemDef("bg_sakura", ShopItemType.BACKGROUND, "벚꽃동산", "꽃잎 흩날리는 분홍 벚꽃동산", 200, "bg/sakura", null, -10, 20, false),
            ItemDef("bg_beach", ShopItemType.BACKGROUND, "바닷가", "파도가 반짝이는 여름 바닷가", 220, "bg/beach", null, -10, 30, false),
            ItemDef("bg_night", ShopItemType.BACKGROUND, "밤하늘", "달과 별이 빛나는 고요한 밤", 240, "bg/night", null, -10, 40, false),
            ItemDef("bg_winter", ShopItemType.BACKGROUND, "눈 내리는 언덕", "소복소복 눈이 쌓이는 겨울 언덕", 260, "bg/winter", null, -10, 50, false),
            ItemDef("bg_space", ShopItemType.BACKGROUND, "우주 여행", "행성 사이를 떠다니는 우주", 300, "bg/space", null, -10, 60, false),
            // ─── GLASSES ────────────────────────────────────────────
            ItemDef("glasses_round", ShopItemType.GLASSES, "동그란 안경", "클래식한 동글이 안경", 100, "item/glasses_round", null, 50, 10, false),
            ItemDef("glasses_heart", ShopItemType.GLASSES, "하트 선글라스", "하트 모양 선글라스", 150, "item/glasses_heart", null, 50, 20, false),
            ItemDef("glasses_sun", ShopItemType.GLASSES, "선글라스", "멋쟁이 검정 선글라스", 130, "item/glasses_sun", null, 50, 30, false),
            ItemDef("glasses_star", ShopItemType.GLASSES, "별 선글라스", "무대 체질 별 모양 선글라스", 160, "item/glasses_star", null, 50, 40, false),
            // ─── HAIRPIN ────────────────────────────────────────────
            ItemDef("hairpin_star", ShopItemType.HAIRPIN, "별 머리핀", "반짝이는 별 머리핀", 60, "item/hairpin_star", null, 30, 10, false),
            ItemDef("hairpin_ribbon", ShopItemType.HAIRPIN, "리본 머리핀", "귀여운 리본 머리핀", 80, "item/hairpin_ribbon", null, 30, 20, false),
            ItemDef("hairpin_flower", ShopItemType.HAIRPIN, "꽃 머리핀", "앙증맞은 꽃 머리핀", 90, "item/hairpin_flower", null, 30, 30, false),
            ItemDef("hairpin_clover", ShopItemType.HAIRPIN, "클로버 핀", "행운의 네잎클로버 핀", 70, "item/hairpin_clover", null, 30, 40, false),
            // ─── HAT ────────────────────────────────────────────────
            ItemDef("hat_party", ShopItemType.HAT, "파티 모자", "신나는 파티 모자", 80, "item/hat_party", null, 60, 10, false),
            ItemDef("hat_chef", ShopItemType.HAT, "요리사 모자", "하얀 요리사 모자", 150, "item/hat_chef", null, 60, 20, false),
            ItemDef("hat_straw", ShopItemType.HAT, "밀짚모자", "여름 소풍엔 역시 밀짚모자", 120, "item/hat_straw", null, 60, 30, false),
            ItemDef("hat_beret", ShopItemType.HAT, "베레모", "화가 모찌의 빨간 베레모", 140, "item/hat_beret", null, 60, 40, false),
            ItemDef("hat_wizard", ShopItemType.HAT, "마법사 모자", "별이 수놓인 마법사 모자", 200, "item/hat_wizard", null, 60, 50, false),
            // ─── ACCESSORY ──────────────────────────────────────────
            ItemDef("accessory_bowtie", ShopItemType.ACCESSORY, "보타이", "깔끔한 보타이", 100, "item/bowtie", null, 20, 10, false),
            ItemDef("accessory_scarf", ShopItemType.ACCESSORY, "목도리", "따뜻한 목도리", 120, "item/scarf", null, 20, 20, false),
            ItemDef("accessory_necklace", ShopItemType.ACCESSORY, "진주 목걸이", "반짝이는 진주 목걸이", 140, "item/necklace", null, 20, 30, false),
            ItemDef("accessory_bell", ShopItemType.ACCESSORY, "방울 목걸이", "딸랑딸랑 황금 방울 목걸이", 110, "item/bell", null, 20, 40, false),
            // ─── MISC ───────────────────────────────────────────────
            ItemDef("misc_balloon", ShopItemType.MISC, "풍선", "함께 떠다니는 풍선", 80, "item/balloon", null, 5, 10, false),
            ItemDef("misc_flower", ShopItemType.MISC, "꽃 한송이", "곁에 둔 꽃 한송이", 60, "item/flower", null, 5, 20, false),
            ItemDef("misc_star", ShopItemType.MISC, "반짝 별", "곁에서 반짝이는 별", 70, "item/star", null, 5, 30, false),
            ItemDef("misc_heart_balloon", ShopItemType.MISC, "하트 풍선", "두둥실 떠다니는 하트 풍선", 90, "item/balloon_heart", null, 5, 40, false),
            ItemDef("misc_butterfly", ShopItemType.MISC, "나비 친구", "곁을 맴도는 노랑나비", 110, "item/butterfly", null, 5, 50, false),
            ItemDef("misc_music", ShopItemType.MISC, "음표", "기분 좋은 날의 콧노래", 60, "item/music_note", null, 5, 60, false)
        )
    }
}
