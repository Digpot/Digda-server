package digdaserver.domain.event.domain.repository

import digdaserver.domain.event.domain.entity.CoinGrant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CoinGrantRepository : JpaRepository<CoinGrant, Long> {

    fun findAllByOrderByIdDesc(pageable: Pageable): Page<CoinGrant>
}
