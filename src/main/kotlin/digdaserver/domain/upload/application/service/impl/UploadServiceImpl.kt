package digdaserver.domain.upload.application.service.impl

import digdaserver.domain.upload.application.service.UploadService
import digdaserver.domain.upload.domain.entity.ImagePurpose
import digdaserver.domain.upload.domain.entity.UploadedImage
import digdaserver.domain.upload.domain.repository.UploadedImageRepository
import digdaserver.domain.upload.presentation.dto.res.UploadImageResponse
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.s3.presentation.application.S3Service
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import javax.imageio.ImageIO
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UploadServiceImpl(
    private val uploadedImageRepository: UploadedImageRepository,
    private val userRepository: UserRepository,
    private val s3Service: S3Service
) : UploadService {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_SIZE_BYTES = 8L * 1024 * 1024
        private val ALLOWED_CONTENT_TYPES = setOf("image/png", "image/jpeg", "image/jpg")
    }

    @Transactional
    override fun uploadImage(userId: UUID, file: MultipartFile, purpose: String): UploadImageResponse {
        validateFile(file)

        val imagePurpose = parsePurpose(purpose)

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val (width, height) = readImageDimensions(file)

        val url = s3Service.storeImage(file, userId.toString())
            ?: throw DigdaException(ErrorCode.SERVER_ERROR)

        val saved = uploadedImageRepository.save(
            UploadedImage(
                user = user,
                url = url,
                width = width,
                height = height,
                purpose = imagePurpose
            )
        )

        return UploadImageResponse.from(saved)
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) throw DigdaException(ErrorCode.INVALID_FILE_TYPE)
        if (file.size > MAX_SIZE_BYTES) throw DigdaException(ErrorCode.FILE_TOO_LARGE)

        val contentType = file.contentType?.lowercase()
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw DigdaException(ErrorCode.INVALID_FILE_TYPE)
        }
    }

    private fun parsePurpose(purpose: String): ImagePurpose {
        return when (purpose.lowercase()) {
            "profile" -> ImagePurpose.PROFILE
            "group_thumbnail" -> ImagePurpose.GROUP_THUMBNAIL
            "diary" -> ImagePurpose.DIARY
            else -> throw DigdaException(ErrorCode.INVALID_PARAMETER)
        }
    }

    private fun readImageDimensions(file: MultipartFile): Pair<Int, Int> {
        return try {
            file.inputStream.use { stream ->
                val image = ImageIO.read(stream) ?: return 0 to 0
                image.width to image.height
            }
        } catch (e: Exception) {
            log.warn("Failed to read image dimensions for file '{}': {}", file.originalFilename, e.message)
            0 to 0
        }
    }
}
