package digdaserver.domain.device.domain.repository

import digdaserver.domain.device.domain.entity.Device
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface DeviceRepository : JpaRepository<Device, Long> {

    fun findByToken(token: String): Optional<Device>

    fun findAllByUserId(userId: Long): List<Device>

    fun deleteAllByUserId(userId: Long)
}
