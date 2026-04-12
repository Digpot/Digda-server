package digdaserver.domain.user.application.service

import digdaserver.domain.user.presentation.dto.req.UpdateProfileRequest
import digdaserver.domain.user.presentation.dto.res.MyProfileResponse
import java.util.UUID

interface UserProfileService {

    fun getMyProfile(userId: UUID): MyProfileResponse

    fun updateProfile(userId: UUID, request: UpdateProfileRequest): MyProfileResponse
}
