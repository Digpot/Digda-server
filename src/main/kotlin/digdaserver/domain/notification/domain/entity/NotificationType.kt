package digdaserver.domain.notification.domain.entity

enum class NotificationType {

    SCHEDULE_CREATED,
    SCHEDULE_UPDATED,
    DIARY_WRITTEN,
    COMMENT_ON_SCHEDULE,
    COMMENT_ON_DIARY,
    MEMBER_JOINED,
    MEMBER_LEFT,
    MEMBER_REMOVED,
    OWNERSHIP_TRANSFERRED,
    GROUP_DELETE_SCHEDULED;
}
