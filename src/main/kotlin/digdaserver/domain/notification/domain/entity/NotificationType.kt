package digdaserver.domain.notification.domain.entity

import com.fasterxml.jackson.annotation.JsonValue

enum class NotificationType {

    SCHEDULE_CREATED,
    SCHEDULE_UPDATED,
    SCHEDULE_DAY_BEFORE,
    SCHEDULE_TODAY,
    DIARY_WRITTEN,
    COMMENT_ON_SCHEDULE,
    COMMENT_ON_DIARY,
    MEMBER_JOINED,
    MEMBER_LEFT,
    MEMBER_REMOVED,
    OWNERSHIP_TRANSFERRED,
    GROUP_DELETE_SCHEDULED,
    QUIZ_CREATED,
    QUIZ_ANSWERED,
    MOCHI_LEVELUP,
    DIKO_UNLOCKED,
    GAME_INVITE,
    INQUIRY_ANSWERED,
    ANNOUNCEMENT;

    @JsonValue
    fun toValue(): String = name.lowercase()
}
