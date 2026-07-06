package digdaserver.global.infra.s3.presentation.application

import org.springframework.web.multipart.MultipartFile

interface S3Service {

    // 프로필 이미지 업로드
    fun storeImage(multipartFile: MultipartFile, userId: String): String?

    // 정적 지도 이미지 저장
    fun storeStaticMapImage(
        key: String,
        bytes: ByteArray,
        contentType: String
    ): String
}
