package digdaserver.domain.character.domain.entity

/**
 * 모찌 상점 아이템 분류.
 *
 * 각 [ShopItemType] 은 카테고리당 1개 장착 슬롯을 차지한다 — 예를 들어 [GLASSES]
 * 슬롯에는 동시에 한 종류의 안경만 착용 가능하다. 슬롯 모델이라 새 카테고리는 enum
 * 뒤에 append 만 하면 되고 마이그레이션이 필요 없다.
 *
 * 클라이언트는 enum 키만 식별자로 사용하고, 표시명·정렬은 [ShopItem] 마스터에서
 * 받는다 (디자인 변경 시 앱 배포 없이 조정 가능).
 */
enum class ShopItemType(val displayName: String, val slotOrder: Int) {
    /** 캐릭터 전체 외형(배경 squircle 색·바디 톤). 항상 1개 장착(default). */
    SKIN("스킨", 0),

    /** 머리 위 모자. */
    HAT("모자", 1),

    /** 눈 부위 안경/선글라스. */
    GLASSES("안경", 2),

    /** 머리 옆 머리핀. */
    HAIRPIN("머리핀", 3),

    /** 목 부근 액세서리(보타이·목도리 등). */
    ACCESSORY("액세서리", 4),

    /** 캐릭터 옆에 둘러진 잡화(풍선·꽃 등). */
    MISC("잡화", 5);

    companion object {
        fun safeValueOf(raw: String): ShopItemType? = entries.firstOrNull { it.name == raw }
    }
}
