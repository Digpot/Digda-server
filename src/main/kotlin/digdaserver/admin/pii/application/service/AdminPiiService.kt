package digdaserver.admin.pii.application.service

import digdaserver.admin.pii.presentation.dto.req.RevealPiiRequest
import digdaserver.admin.pii.presentation.dto.res.RevealPiiResponse
import java.util.UUID

interface AdminPiiService {

    fun reveal(adminUserId: UUID, request: RevealPiiRequest): RevealPiiResponse
}
