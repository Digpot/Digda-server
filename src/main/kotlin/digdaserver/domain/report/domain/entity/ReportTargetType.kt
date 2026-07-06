package digdaserver.domain.report.domain.entity

/**
 * 신고 대상 종류.
 *
 * - [DIARY] / [COMMENT] / [SCHEDULE] : targetId 는 해당 엔티티의 Long PK 문자열.
 * - [USER] : targetId 는 사용자 UUID 문자열(프로필 신고).
 */
enum class ReportTargetType {
    DIARY,
    COMMENT,
    SCHEDULE,
    USER
}
