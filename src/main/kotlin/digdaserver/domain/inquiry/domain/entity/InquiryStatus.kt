package digdaserver.domain.inquiry.domain.entity

/** 고객센터 문의 처리 상태. */
enum class InquiryStatus {
    /** 접수됨(미답변). */
    PENDING,

    /** 답변/처리 완료. */
    ANSWERED
}
