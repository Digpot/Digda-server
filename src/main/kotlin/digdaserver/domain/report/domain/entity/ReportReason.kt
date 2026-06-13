package digdaserver.domain.report.domain.entity

/** 신고 사유. 앱/어드민에서 같은 키를 공유한다. */
enum class ReportReason {
    /** 스팸·광고·도배 */
    SPAM,

    /** 욕설·비방·괴롭힘 */
    ABUSE,

    /** 음란물·선정성 */
    SEXUAL,

    /** 폭력·혐오 */
    VIOLENCE,

    /** 개인정보 노출 */
    PRIVACY,

    /** 기타(상세 사유 동봉) */
    ETC
}
