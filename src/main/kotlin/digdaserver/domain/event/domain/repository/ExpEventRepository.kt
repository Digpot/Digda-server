package digdaserver.domain.event.domain.repository

import digdaserver.domain.event.domain.entity.ExpEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExpEventRepository : JpaRepository<ExpEvent, Long> {

    /** 단일 행 운영 — app_config 와 동일하게 가장 먼저 만들어진 행 하나만 사용한다. */
    fun findFirstByOrderByIdAsc(): ExpEvent?
}
