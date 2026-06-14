package digdaserver.admin.report.presentation.dto.res

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 신고된 콘텐츠의 원본 스냅샷 — 어드민이 신고를 검토할 때 실제 내용을 함께 본다.
 * 종류별로 채워지는 필드가 다르다.
 * - DIARY:    title + text(본문) + images(사진들)
 * - SCHEDULE: title + text(기간/시간 요약)
 * - COMMENT:  text(댓글 내용)
 * - USER:     content 없음(available=false)
 * 콘텐츠가 이미 삭제됐으면 available=false 로 내려 어드민에서 '원본 없음' 표기.
 */
@Schema(description = "신고된 콘텐츠 원본 스냅샷")
data class AdminReportTargetContentResponse(

    @Schema(description = "원본이 남아 있는지(삭제·해석불가 시 false)")
    val available: Boolean,

    @Schema(description = "제목(일기/일정)")
    val title: String? = null,

    @Schema(description = "본문/내용(일기 본문·댓글 내용·일정 요약)")
    val text: String? = null,

    @Schema(description = "사진 URL 목록(일기)")
    val images: List<String> = emptyList(),

    @Schema(description = "작성자 이름")
    val authorName: String? = null,

    @Schema(description = "작성 시각")
    val createdAt: LocalDateTime? = null
) {
    companion object {
        /** 원본을 찾지 못했을 때(삭제됨/USER 대상 등). */
        fun unavailable(): AdminReportTargetContentResponse =
            AdminReportTargetContentResponse(available = false)
    }
}
