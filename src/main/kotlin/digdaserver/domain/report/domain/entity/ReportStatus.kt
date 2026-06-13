package digdaserver.domain.report.domain.entity

/** 신고 처리 상태. 어드민(digda-admin)에서 전이시킨다. */
enum class ReportStatus {
    /** 접수됨(미처리) */
    PENDING,

    /** 검토 후 조치 완료(콘텐츠 삭제 등) */
    RESOLVED,

    /** 검토 후 반려(문제 없음) */
    DISMISSED
}
