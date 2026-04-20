package digdaserver.domain.upload.application.service

import digdaserver.domain.upload.presentation.dto.res.UploadImageResponse
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

interface UploadService {

    fun uploadImage(userId: UUID, file: MultipartFile, purpose: String): UploadImageResponse
}
