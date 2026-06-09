package digdaserver.domain.appconfig.domain.repository

import digdaserver.domain.appconfig.domain.entity.AppConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AppConfigRepository : JpaRepository<AppConfig, Long> {

    /** 단일 행 운영 — 가장 먼저 생성된 1행. */
    fun findFirstByOrderByIdAsc(): AppConfig?
}
