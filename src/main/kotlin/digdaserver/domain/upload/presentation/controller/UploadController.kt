package digdaserver.domain.upload.presentation.controller

import digdaserver.domain.upload.application.service.UploadService
import digdaserver.domain.upload.presentation.dto.res.UploadImageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@Tag(name = "Upload", description = "이미지 업로드 API")
class UploadController(
    private val uploadService: UploadService
) {

    @Operation(summary = "이미지 업로드", description = "프로필/그룹방 썸네일/일기 이미지를 업로드합니다. PNG/JPEG만 지원, 최대 5MB.")
    @PostMapping("/uploads/images", consumes = ["multipart/form-data"])
    fun uploadImage(
        @AuthenticationPrincipal userId: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("purpose") purpose: String
    ): ResponseEntity<UploadImageResponse> {
        val response = uploadService.uploadImage(UUID.fromString(userId), file, purpose)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
