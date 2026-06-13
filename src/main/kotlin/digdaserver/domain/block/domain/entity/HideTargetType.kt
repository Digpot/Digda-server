package digdaserver.domain.block.domain.entity

/** 개별 콘텐츠 숨김 대상 종류. 사용자 단위 차단은 [UserBlock] 로 별도 관리한다. */
enum class HideTargetType {
    DIARY,
    COMMENT,
    SCHEDULE
}
