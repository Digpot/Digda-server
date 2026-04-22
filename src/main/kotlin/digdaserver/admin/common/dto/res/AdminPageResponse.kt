package digdaserver.admin.common.dto.res

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(description = "어드민 페이지네이션 응답")
data class AdminPageResponse<T>(

    @Schema(description = "현재 페이지(0-base)")
    val page: Int,

    @Schema(description = "페이지 크기")
    val size: Int,

    @Schema(description = "총 요소 수")
    val totalElements: Long,

    @Schema(description = "총 페이지 수")
    val totalPages: Int,

    @Schema(description = "데이터 목록")
    val content: List<T>
) {
    companion object {
        fun <T> of(page: Page<T>): AdminPageResponse<T> = AdminPageResponse(
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            content = page.content
        )

        fun <T, R> of(page: Page<T>, mapper: (T) -> R): AdminPageResponse<R> = AdminPageResponse(
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            content = page.content.map(mapper)
        )
    }
}
