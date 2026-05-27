package digdaserver.domain.character.domain.entity

/**
 * 모찌 색상. CORAL 은 기본 지급(coin=0), 그 외는 상점 구매.
 *
 * 클라이언트는 enum 키로 색을 식별하고, 표시용 hex/name 은 서버 응답에서 함께 제공해
 * 디자인 변경 시 앱 배포 없이 톤 조정이 가능하게 한다.
 */
enum class CharacterColor(
    val displayName: String,
    val hex: String,
    val cost: Int,
    val isDefault: Boolean
) {
    CORAL("코랄", "#FF6B6B", 0, true),
    MINT("민트", "#34D399", 120, false),
    LAVENDER("라벤더", "#A78BFA", 150, false),
    BUTTER("버터", "#FCD34D", 100, false),
    MIDNIGHT("미드나잇", "#1F2937", 200, false);
}
