package digdaserver.domain.oauth2.application.service

interface LogoutService {
    fun logout(userId: String)
}
