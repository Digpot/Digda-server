package digdaserver.admin.user.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.user.application.service.AdminUserService
import digdaserver.admin.user.presentation.dto.res.AdminUserResponse
import digdaserver.domain.user.domain.entity.Role
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AdminUserServiceImpl(
    private val userRepository: UserRepository
) : AdminUserService {

    override fun search(keyword: String?, role: Role?, page: Int, size: Int): AdminPageResponse<AdminUserResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = userRepository.searchForAdmin(keyword, role, pageable)
        return AdminPageResponse.of(result, AdminUserResponse::from)
    }

    override fun getDetail(userId: UUID): AdminUserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }
        return AdminUserResponse.from(user)
    }

    @Transactional
    override fun updateRole(userId: UUID, role: Role): AdminUserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }
        user.role = role
        return AdminUserResponse.from(user)
    }
}
