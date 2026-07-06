package digdaserver.domain.deletionrequest.domain.entity

/** 삭제 요청 종류 — 계정 전체 삭제 / 데이터 일부 삭제. */
enum class DeletionRequestType {
    ACCOUNT,
    DATA
}
