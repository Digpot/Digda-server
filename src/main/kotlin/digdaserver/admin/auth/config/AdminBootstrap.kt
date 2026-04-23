package digdaserver.admin.auth.config

import digdaserver.admin.auth.domain.entity.AdminCredential
import digdaserver.admin.auth.domain.repository.AdminCredentialRepository
import digdaserver.domain.oauth2.domain.entity.SocialProvider
import digdaserver.domain.user.domain.entity.Role
import digdaserver.domain.user.domain.entity.User
import digdaserver.domain.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("dev")
class AdminBootstrap(
    private val userRepository: UserRepository,
    private val adminCredentialRepository: AdminCredentialRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationListener<ApplicationReadyEvent> {

    private val log = LoggerFactory.getLogger(AdminBootstrap::class.java)

    @Transactional
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val email = System.getenv("DEFAULT_ADMIN_EMAIL") ?: "admin@digda.com"
        val rawPassword = System.getenv("DEFAULT_ADMIN_PASSWORD") ?: "qkek@@0312"
        val name = System.getenv("DEFAULT_ADMIN_NAME") ?: "admin"

        if (adminCredentialRepository.findByEmail(email).isPresent) {
            log.info("[AdminBootstrap] 기존 관리자 존재 — 생성 스킵 (email={})", email)
            return
        }

        val user = userRepository.save(
            User(
                email = email,
                name = name,
                socialProvider = SocialProvider.ADMIN,
                role = Role.ADMIN
            )
        )

        adminCredentialRepository.save(
            AdminCredential(
                user = user,
                email = email,
                password = passwordEncoder.encode(rawPassword)
            )
        )

        log.info("[AdminBootstrap] 기본 관리자 생성 완료 (email={}, userId={})", email, user.id)
    }
}
