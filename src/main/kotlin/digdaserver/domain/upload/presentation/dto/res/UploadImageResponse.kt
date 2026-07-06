package digdaserver.domain.upload.presentation.dto.res

import digdaserver.domain.upload.domain.entity.UploadedImage

data class UploadImageResponse(
    val id: Long,
    val url: String,
    val width: Int,
    val height: Int
) {
    companion object {
        fun from(image: UploadedImage): UploadImageResponse = UploadImageResponse(
            id = image.id,
            url = image.url,
            width = image.width,
            height = image.height
        )
    }
}
