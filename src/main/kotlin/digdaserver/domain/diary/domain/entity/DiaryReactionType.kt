package digdaserver.domain.diary.domain.entity

/**
 * 일기에 달 수 있는 이모지 리액션 종류.
 * 클라이언트는 enum name 문자열로 주고받는다.
 */
enum class DiaryReactionType {
    HEART, // ❤️
    CRY, // 🥹
    SPARKLE, // ✨
    LAUGH, // 😆
    FIRE // 🔥
}
