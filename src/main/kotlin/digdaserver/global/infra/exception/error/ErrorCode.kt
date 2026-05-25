package digdaserver.global.infra.exception.error

enum class ErrorCode(
    val code: String,
    val message: String,
    val httpCode: Int
) {

    // ── Common ──
    SERVER_ERROR("SERVER_ERROR", "서버 내부 오류가 발생했습니다.", 500),
    INVALID_PARAMETER("INVALID_PARAMETER", "잘못된 파라미터입니다.", 400),
    PARAMETER_VALIDATION_ERROR("PARAMETER_VALIDATION_ERROR", "파라미터 검증 에러입니다.", 400),
    PARAMETER_GRAMMAR_ERROR("PARAMETER_GRAMMAR_ERROR", "잘못된 요청 형식입니다.", 400),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.", 404),

    // ── Auth / JWT ──
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", 401),
    FORBIDDEN("FORBIDDEN", "권한이 없습니다.", 403),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "토큰이 만료되었습니다. 재로그인이 필요합니다.", 401),
    TOKEN_INVALID("TOKEN_INVALID", "유효하지 않은 토큰입니다.", 401),
    ACCESS_TOKEN_INVALID("ACCESS_TOKEN_INVALID", "Access Token이 유효하지 않습니다.", 401),
    REFRESH_TOKEN_INVALID("REFRESH_TOKEN_INVALID", "Refresh Token이 유효하지 않습니다.", 401),
    REFRESH_TOKEN_NOT_FOUND("REFRESH_TOKEN_NOT_FOUND", "Refresh Token이 존재하지 않습니다.", 401),
    DUPLICATE_LOGIN("DUPLICATE_LOGIN", "이미 로그인된 상태입니다.", 409),

    // ── OAuth2 ──
    INVALID_PROVIDER("INVALID_PROVIDER", "지원하지 않는 소셜 로그인 제공자입니다.", 400),
    APPLE_JWT_ERROR("APPLE_JWT_ERROR", "Apple JWT 처리 중 오류가 발생했습니다.", 500),
    APPLE_KEY_PARSE_ERROR("APPLE_KEY_PARSE_ERROR", "Apple 공개키 파싱에 실패했습니다.", 401),
    ID_TOKEN_INVALID("ID_TOKEN_INVALID", "잘못된 ID 토큰입니다.", 401),
    SOCIAL_AUTH_FAILED("SOCIAL_AUTH_FAILED", "소셜 인증에 실패했습니다.", 401),

    // ── Auth (온보딩) ──
    REQUIRED_TERMS_NOT_AGREED("REQUIRED_TERMS_NOT_AGREED", "필수 약관에 동의해야 합니다.", 400),

    // ── User ──
    USER_NOT_FOUND("USER_NOT_FOUND", "존재하지 않는 사용자입니다.", 404),
    DUPLICATE_EMAIL("DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다.", 409),
    NAME_TOO_SHORT("NAME_TOO_SHORT", "닉네임은 2자 이상이어야 합니다.", 400),
    NAME_TOO_LONG("NAME_TOO_LONG", "닉네임은 20자 이하여야 합니다.", 400),
    STATUS_MESSAGE_TOO_LONG("STATUS_MESSAGE_TOO_LONG", "상태 메시지는 100자 이하여야 합니다.", 400),
    INVALID_ROLE("INVALID_ROLE", "유효하지 않은 역할입니다.", 400),

    // ── GroupRoom ──
    GROUP_ROOM_NOT_FOUND("GROUP_ROOM_NOT_FOUND", "존재하지 않는 그룹방입니다.", 404),
    GROUP_ROOM_NAME_TOO_SHORT("GROUP_ROOM_NAME_TOO_SHORT", "그룹방명은 2자 이상이어야 합니다.", 400),
    GROUP_ROOM_NAME_TOO_LONG("GROUP_ROOM_NAME_TOO_LONG", "그룹방명은 20자 이하여야 합니다.", 400),
    MAX_MEMBERS_BELOW_CURRENT("MAX_MEMBERS_BELOW_CURRENT", "현재 구성원 수보다 적은 값으로 설정할 수 없습니다.", 400),
    GROUP_ROOM_NOT_SCHEDULED_FOR_DELETION("GROUP_ROOM_NOT_SCHEDULED_FOR_DELETION", "삭제 예약되지 않은 그룹방입니다.", 400),
    GROUP_ROOM_ALREADY_DELETED("GROUP_ROOM_ALREADY_DELETED", "이미 삭제된 그룹방입니다.", 410),
    OWNS_ACTIVE_GROUP_ROOM("OWNS_ACTIVE_GROUP_ROOM", "소유 중인 그룹방이 있어 탈퇴할 수 없습니다. 방장을 양도해주세요.", 409),

    // ── Invite ──
    INVITE_CODE_INVALID("INVITE_CODE_INVALID", "존재하지 않는 초대 코드입니다.", 404),
    INVITE_CODE_EXPIRED("INVITE_CODE_EXPIRED", "만료된 초대 코드입니다.", 410),
    GROUP_ROOM_FULL("GROUP_ROOM_FULL", "그룹방 인원이 초과되었습니다.", 409),
    ALREADY_JOINED("ALREADY_JOINED", "이미 참여 중인 그룹방입니다.", 409),

    // ── Membership ──
    NOT_GROUP_ROOM_MEMBER("NOT_GROUP_ROOM_MEMBER", "해당 그룹방의 구성원이 아닙니다.", 403),
    NOT_GROUP_ROOM_OWNER("NOT_GROUP_ROOM_OWNER", "방장 권한이 필요합니다.", 403),
    CANNOT_REMOVE_OWNER("CANNOT_REMOVE_OWNER", "방장은 내보낼 수 없습니다.", 400),
    USER_NOT_IN_GROUP_ROOM("USER_NOT_IN_GROUP_ROOM", "해당 그룹방 구성원이 아닙니다.", 404),
    OWNER_CANNOT_LEAVE("OWNER_CANNOT_LEAVE", "방장은 양도 후 탈퇴할 수 있습니다.", 400),

    // ── Schedule ──
    SCHEDULE_NOT_FOUND("SCHEDULE_NOT_FOUND", "존재하지 않는 일정입니다.", 404),
    END_DATE_BEFORE_START("END_DATE_BEFORE_START", "종료일이 시작일보다 이전입니다.", 400),
    END_TIME_BEFORE_START("END_TIME_BEFORE_START", "종료 시간이 시작 시간보다 이전입니다.", 400),
    INVALID_PARTICIPANT("INVALID_PARTICIPANT", "참여자가 그룹 구성원이 아닙니다.", 400),

    // ── Diary ──
    DIARY_NOT_FOUND("DIARY_NOT_FOUND", "존재하지 않는 일기입니다.", 404),
    FUTURE_DATE_NOT_ALLOWED("FUTURE_DATE_NOT_ALLOWED", "미래 날짜에는 일기를 작성할 수 없습니다.", 400),
    INVALID_WEATHER_VALUE("INVALID_WEATHER_VALUE", "날씨 값은 0~3 범위여야 합니다.", 400),
    INVALID_MOOD_VALUE("INVALID_MOOD_VALUE", "기분 값은 0~3 범위여야 합니다.", 400),

    // ── Comment ──
    COMMENT_NOT_FOUND("COMMENT_NOT_FOUND", "존재하지 않는 댓글입니다.", 404),
    COMMENT_TOO_LONG("COMMENT_TOO_LONG", "댓글은 200자 이하여야 합니다.", 400),

    // ── Todo ──
    TODO_NOT_FOUND("TODO_NOT_FOUND", "존재하지 않는 할 일입니다.", 404),
    TODO_TEXT_TOO_LONG("TODO_TEXT_TOO_LONG", "할 일은 100자 이하여야 합니다.", 400),

    // ── Notification ──
    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", "존재하지 않는 알림입니다.", 404),
    NOTIFICATION_SETTING_NOT_FOUND("NOTIFICATION_SETTING_NOT_FOUND", "알림 설정을 찾을 수 없습니다.", 404),

    // ── Device ──
    DEVICE_NOT_FOUND("DEVICE_NOT_FOUND", "존재하지 않는 디바이스입니다.", 404),

    // ── Upload ──
    FILE_TOO_LARGE("FILE_TOO_LARGE", "사진 용량이 너무 커요. 50MB 이하로 올려주세요.", 413),
    INVALID_FILE_TYPE("INVALID_FILE_TYPE", "PNG 또는 JPEG 사진만 올릴 수 있어요.", 400),
    IMAGE_NOT_FOUND("IMAGE_NOT_FOUND", "존재하지 않는 이미지입니다.", 404),

    // ── Rate Limit ──
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", "요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.", 429),

    // ── Admin ──
    ADMIN_NOT_FOUND("ADMIN_NOT_FOUND", "존재하지 않는 관리자 계정입니다.", 404),
    ADMIN_PASSWORD_MISMATCH("ADMIN_PASSWORD_MISMATCH", "비밀번호가 일치하지 않습니다.", 401),
    NOT_ADMIN_USER("NOT_ADMIN_USER", "관리자 권한이 없는 사용자입니다.", 403),
    ADMIN_TABLE_NOT_ALLOWED("ADMIN_TABLE_NOT_ALLOWED", "조회할 수 없는 테이블명입니다.", 400),
    ADMIN_TABLE_NOT_FOUND("ADMIN_TABLE_NOT_FOUND", "존재하지 않는 테이블입니다.", 404),
    ADMIN_COLUMN_NOT_ALLOWED("ADMIN_COLUMN_NOT_ALLOWED", "유효하지 않은 컬럼명입니다.", 400),
    ADMIN_PK_NOT_FOUND("ADMIN_PK_NOT_FOUND", "테이블에 PK가 없어 단일 행 수정/삭제가 불가합니다.", 400),
    ADMIN_PK_VALUE_MISSING("ADMIN_PK_VALUE_MISSING", "PK 값이 누락되었습니다.", 400),
    ADMIN_ROW_NOT_FOUND("ADMIN_ROW_NOT_FOUND", "해당 PK의 행을 찾을 수 없습니다.", 404),
    ADMIN_ROW_AFFECTED_INVALID("ADMIN_ROW_AFFECTED_INVALID", "행 수정/삭제에서 1행이 아닌 결과가 발생했습니다.", 500),
    ADMIN_NO_FIELDS_TO_UPDATE("ADMIN_NO_FIELDS_TO_UPDATE", "수정할 컬럼이 없습니다.", 400);
}
