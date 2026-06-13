package digdaserver.domain.block.domain.entity

/**
 * 개별 콘텐츠가 숨겨진 이유. 앱에서 플레이스홀더 문구를 분기하는 데 쓴다.
 *
 * - [REPORTED] : 신고하면서 본인에게서 자동 숨김된 경우.
 * - [HIDDEN]   : 신고 없이 "이 게시물 숨기기"로 직접 숨긴 경우.
 */
enum class HideReason {
    REPORTED,
    HIDDEN
}
