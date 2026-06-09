package digdaserver.domain.title.presentation.dto.res

import digdaserver.domain.title.domain.entity.TitleCatalogEntry
import io.swagger.v3.oas.annotations.media.Schema

/** 칭호 카탈로그 1종(표시 메타 + 획득 조건). 앱·어드민이 code 로 공유한다. */
@Schema(description = "칭호 카탈로그 항목")
data class TitleCatalogResponse(
    val code: String,
    val name: String,
    val description: String,
    val category: String,
    val accentColor: String,
    val iconKey: String,
    val conditionType: String,
    val conditionValue: String?,
    val sortOrder: Int
) {
    companion object {
        fun from(e: TitleCatalogEntry): TitleCatalogResponse = TitleCatalogResponse(
            code = e.code,
            name = e.name,
            description = e.description,
            category = e.category,
            accentColor = e.accentColor,
            iconKey = e.iconKey,
            conditionType = e.conditionType,
            conditionValue = e.conditionValue,
            sortOrder = e.sortOrder
        )
    }
}
