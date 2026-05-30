package digdaserver.admin.auth.config

import digdaserver.admin.auth.domain.entity.AdminCredential
import digdaserver.admin.auth.domain.repository.AdminCredentialRepository
import digdaserver.domain.oauth2.domain.entity.SocialProvider
import digdaserver.domain.user.domain.entity.Role
import digdaserver.domain.user.domain.entity.User
import digdaserver.domain.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Profile("dev", "prod")
class AdminBootstrap(
    private val userRepository: UserRepository,
    private val adminCredentialRepository: AdminCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${admin.bootstrap.email}") private val email: String,
    @Value("\${admin.bootstrap.password}") private val rawPassword: String,
    @Value("\${admin.bootstrap.name}") private val name: String
) : ApplicationListener<ApplicationReadyEvent> {

    private val log = LoggerFactory.getLogger(AdminBootstrap::class.java)

    @Transactional
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        ensureBootstrapAdmin()
        demoteOrphanAdmins()
    }

    /** 부트스트랩 관리자(이메일 유니크) 가 없으면 1회 생성. 있으면 스킵 — 멱등. */
    private fun ensureBootstrapAdmin() {
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

    /**
     * 관리자(ADMIN) 는 admin_credential 을 가진 계정만 실제로 로그인 가능하다.
     * 자격증명 없이 role=ADMIN 으로만 떠 있는 "유령 관리자" 행은 로그인 불가하면서도
     * 관리자 목록/카운트를 부풀리므로, **삭제하지 않고 USER 로 강등**해 관리자를 실제
     * 로그인 가능한 계정 수와 일치시킨다. (보통 1명)
     */
    private fun demoteOrphanAdmins() {
        val credentialedUserIds: Set<UUID> =
            adminCredentialRepository.findAll().map { it.user.id }.toSet()
        val orphanAdmins = userRepository.findAllByRole(Role.ADMIN)
            .filter { it.id !in credentialedUserIds }
        if (orphanAdmins.isEmpty()) return

        orphanAdmins.forEach { it.demoteToUser() }
        log.warn(
            "[AdminBootstrap] 자격증명 없는 ADMIN {}건을 USER 로 강등(삭제 아님) — 관리자 정리, ids={}",
            orphanAdmins.size,
            orphanAdmins.map { it.id }
        )
    }
}
