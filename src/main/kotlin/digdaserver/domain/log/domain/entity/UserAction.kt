package digdaserver.domain.log.domain.entity

enum class UserAction {
    LOGIN,
    SIGNUP,
    LOGOUT,
    CREATE_DIARY,
    DELETE_DIARY,
    CREATE_SCHEDULE,
    DELETE_SCHEDULE,
    CREATE_COMMENT,
    CREATE_GROUP_ROOM,
    JOIN_GROUP_ROOM,
    LEAVE_GROUP_ROOM,
    TRANSFER_OWNER,
    CREATE_TODO,
    OTHER
}
