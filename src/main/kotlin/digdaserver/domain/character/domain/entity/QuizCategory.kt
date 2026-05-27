package digdaserver.domain.character.domain.entity

/**
 * 캐릭터 퀴즈 카테고리. 신규 카테고리는 enum 뒤에 append 만(서버는 STRING 매핑).
 * 클라이언트가 새 키를 받을 때 unknown 으로 폴백할 수 있도록 displayName 도 함께 제공.
 */
enum class QuizCategory(val displayName: String) {
    PERSONAL("개인"),
    MEMORY("추억"),
    HOBBY("취미"),
    FAVORITE("좋아하는 것"),
    GENERAL("일반");
}
