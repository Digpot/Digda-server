package digdaserver.domain.user.application.service.impl

import digdaserver.domain.upload.domain.repository.UploadedImageRepository
import digdaserver.domain.user.application.service.UserProfileService
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.domain.user.presentation.dto.req.UpdateProfileRequest
import digdaserver.domain.user.presentation.dto.res.MyProfileResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserProfileServiceImpl(
    private val userRepository: UserRepository,
    private val uploadedImageRepository: UploadedImageRepository
) : UserProfileService {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 업로드 PK 문자열 → 실제 S3 URL 로 변환. 그룹방 썸네일과 동일 로직. */
    private fun resolveImageUrl(imageId: String?): String? {
        if (imageId.isNullOrBlank()) return null
        val id = imageId.toLongOrNull() ?: run {
            log.warn("imageId 가 Long 이 아님: imageId={}", imageId)
            return null
        }
        val image = uploadedImageRepository.findById(id).orElse(null)
        if (image == null) {
            log.warn("imageId 에 해당하는 업로드 레코드 없음: imageId={}", id)
            return null
        }
        return image.url
    }

    override fun getMyProfile(userId: UUID): MyProfileResponse {
        log.info("userId={}, action=내 프로필 조회", userId)
        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        return MyProfileResponse.from(user)
    }

    @Transactional
    override fun updateProfile(userId: UUID, request: UpdateProfileRequest): MyProfileResponse {
        log.info(
            "userId={}, action=프로필 수정 요청, fields=[name={}, profileImageId={}]",
            userId,
            request.name,
            request.profileImageId
        )

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        request.name?.let { name ->
            if (name.length < 2) throw DigdaException(ErrorCode.NAME_TOO_SHORT)
            if (name.length > 20) throw DigdaException(ErrorCode.NAME_TOO_LONG)
            user.name = name
        }

        // profileImageId: null(미전송)=변경없음, Optional.empty=초기화, Optional(값)=변경.
        // 값이 들어오면 UploadedImage 의 PK 문자열이므로 S3 URL 로 변환해 저장한다.
        request.profileImageId?.let { optional ->
            if (optional.isPresent) {
                val resolvedUrl = resolveImageUrl(optional.get())
                if (resolvedUrl != null) {
                    user.profileImage = resolvedUrl
                } else {
                    log.warn(
                        "userId={}, action=프로필 이미지 변경 무시(업로드 lookup 실패), imageId={}",
                        userId,
                        optional.get()
                    )
                }
            } else {
                user.resetProfileImage()
            }
        }

        log.info("userId={}, action=프로필 수정 완료, profileImage={}", userId, user.profileImage)
        return MyProfileResponse.from(user)
    }
}
