package digdaserver.domain.character.application.level

/**
 * 레벨업 임계치 곡선.
 *
 *   level n 에서 n+1 로 가는 데 필요한 exp = 100 + (n-1) * 50
 *   → 1->2: 100, 2->3: 150, 3->4: 200, ... 19->20: 1050
 *
 * [MAX_LEVEL]=20 (마스터) 도달 시 추가 exp 는 버려진다 (GroupCharacter.gainExp 참조).
 * 곡선/상한 조정은 여기 한 곳만 바꾸면 됨 — 엔티티/서비스에 흩어 두지 말 것.
 */
object CharacterLevelTable {

    const val MAX_LEVEL: Int = 20
    private const val BASE_REQUIREMENT: Int = 100
    private const val PER_LEVEL_STEP: Int = 50

    fun expForNextLevel(currentLevel: Int): Int {
        if (currentLevel >= MAX_LEVEL) return Int.MAX_VALUE
        val safe = currentLevel.coerceAtLeast(1)
        return BASE_REQUIREMENT + (safe - 1) * PER_LEVEL_STEP
    }
}
