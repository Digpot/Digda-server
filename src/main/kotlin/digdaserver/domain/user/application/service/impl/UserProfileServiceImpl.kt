package digdaserver.domain.user.application.service.impl

import digdaserver.domain.user.application.service.UserProfileService
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.domain.user.presentation.dto.req.UpdateProfileRequest
import digdaserver.domain.user.presentation.dto.res.MyProfileResponse
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserProfileServiceImpl(
    private val userRepository: UserRepository
) : UserProfileService {

    override fun getMyProfile(userId: UUID): MyProfileResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        return MyProfileResponse.from(user)
    }

    @Transactional
    override fun updateProfile(userId: UUID, request: UpdateProfileRequest): MyProfileResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        request.name?.let { name ->
            if (name.length < 2) throw DigdaException(ErrorCode.NAME_TOO_SHORT)
            if (name.length > 20) throw DigdaException(ErrorCode.NAME_TOO_LONG)
            user.name = name
        }

        request.statusMessage?.let { msg ->
            if (msg.isEmpty()) {
                user.clearStatusMessage()
            } else {
                if (msg.length > 100) throw DigdaException(ErrorCode.STATUS_MESSAGE_TOO_LONG)
                user.statusMessage = msg
            }
        }

        request.profileImageId?.let { optional ->
            if (optional.isPresent) {
                user.profileImage = optional.get()
            } else {
                user.resetProfileImage()
            }
        }

        return MyProfileResponse.from(user)
    }
}
