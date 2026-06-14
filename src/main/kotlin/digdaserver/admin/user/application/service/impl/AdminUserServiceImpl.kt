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
        // 키워드가 UUID(user_id) 형태면 정확 매칭으로 단건 조회 — 신고된 사용자를 ID 로 바로 찾기 위함.
        val asUuid = keyword?.trim()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (asUuid != null) {
            val user = userRepository.findById(asUuid).orElse(null)
                ?.takeIf { role == null || it.role == role }
            val content = listOfNotNull(user?.let(AdminUserResponse::from))
            return AdminPageResponse(
                page = 0,
                size = size,
                totalElements = content.size.toLong(),
                totalPages = if (content.isEmpty()) 0 else 1,
                content = content
            )
        }

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

    @Transactional
    override fun updateRestriction(userId: UUID, restricted: Boolean): AdminUserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }
        user.updateRestricted(restricted)
        return AdminUserResponse.from(user)
    }
}
