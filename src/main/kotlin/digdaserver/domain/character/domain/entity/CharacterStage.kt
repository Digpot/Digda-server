package digdaserver.domain.character.domain.entity

/**
 * 모찌 진화 단계.
 *
 * 단계 전환은 [requiredLevel] 도달 시 자동. 한 번 도달한 단계는 되돌아가지 않는다
 * (레벨이 어떤 사유로 하향돼도 stage 자체는 유지) — 사용자 자산이라 보존이 우선.
 *
 * 신규 단계 추가는 enum 뒤에 append 만. 중간 삽입은 DB 의 ordinal 의존이 없는 STRING 매핑
 * 덕분에 안전하지만, [requiredLevel] 충돌은 명시적으로 점검할 것.
 */
enum class CharacterStage(val requiredLevel: Int, val displayName: String) {
    EGG(1, "알 모찌"),
    SPROUT(3, "새싹 모찌"),
    BLOOM(6, "꽃 모찌"),
    BLOSSOM(10, "활짝 모찌"),
    GLOW(15, "빛나는 모찌"),
    MASTER(20, "마스터 모찌");

    companion object {
        /** 주어진 레벨에서 도달 가능한 최고 단계. */
        fun forLevel(level: Int): CharacterStage {
            return entries.lastOrNull { it.requiredLevel <= level } ?: EGG
        }
    }
}
