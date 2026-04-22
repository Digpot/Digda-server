package digdaserver.admin.auth.application.service

import digdaserver.admin.auth.presentation.dto.req.AdminLoginRequest
import digdaserver.admin.auth.presentation.dto.res.AdminLoginResponse

interface AdminAuthService {

    fun login(request: AdminLoginRequest): AdminLoginResponse
}
