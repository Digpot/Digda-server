package digdaserver.domain.oauth2.application.service

import java.util.UUID

interface AccountService {

    fun deleteAccount(userId: UUID)
}
