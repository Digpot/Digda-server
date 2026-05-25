package digdaserver.domain.diary.presentation.dto.res

import digdaserver.domain.diary.domain.entity.DiaryImage

data class DiaryImageResponse(
    val id: Long,
    val url: String,
    val sortOrder: Int
) {
    companion object {
        fun from(image: DiaryImage): DiaryImageResponse = DiaryImageResponse(
            id = image.id,
            url = image.url,
            sortOrder = image.sortOrder
        )
    }
}
