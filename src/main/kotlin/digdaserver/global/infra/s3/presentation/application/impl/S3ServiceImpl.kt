package digdaserver.global.infra.s3.presentation.application.impl

import digdaserver.global.infra.s3.presentation.application.S3Service
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.IOException
import java.util.UUID

@Service
class S3ServiceImpl(
    private val s3Client: S3Client
) : S3Service {

    @Value("\${cloud.aws.s3.bucket}")
    private lateinit var bucketName: String

    @Value("\${cloud.aws.region.static}")
    private lateinit var bucketRegion: String

    private val log = LoggerFactory.getLogger(S3ServiceImpl::class.java)

    override fun storeImage(multipartFile: MultipartFile, userId: String): String? {
        if (multipartFile.isEmpty) {
            return null
        }

        val originalFilename = multipartFile.originalFilename
            ?: throw IOException("파일 이름을 가져올 수 없습니다.")

        val finalFile = createStoreFileName(originalFilename)

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(finalFile)
            .contentType(multipartFile.contentType)
            .build()

        s3Client.putObject(
            putObjectRequest,
            RequestBody.fromInputStream(multipartFile.inputStream, multipartFile.size)
        )

        return "https://$bucketName.s3.$bucketRegion.amazonaws.com/$finalFile"
    }

    override fun storeStaticMapImage(
        key: String,
        bytes: ByteArray,
        contentType: String
    ): String {
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build()

        s3Client.putObject(
            putObjectRequest,
            RequestBody.fromBytes(bytes)
        )

        return "https://$bucketName.s3.$bucketRegion.amazonaws.com/$key"
    }

    private fun createStoreFileName(originalFilename: String): String {
        val ext = extractExt(originalFilename)
        val uuid = UUID.randomUUID().toString()
        return "$uuid.$ext"
    }

    private fun extractExt(originalFilename: String): String {
        val pos = originalFilename.lastIndexOf(".")
        return originalFilename.substring(pos + 1)
    }
}
