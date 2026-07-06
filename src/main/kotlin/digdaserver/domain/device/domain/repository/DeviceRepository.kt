package digdaserver.domain.device.domain.repository

import digdaserver.domain.device.domain.entity.Device
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DeviceRepository : JpaRepository<Device, Long> {

    fun findByToken(token: String): Optional<Device>

    fun findAllByUserId(userId: UUID): List<Device>

    fun findAllByUserIdIn(userIds: Collection<UUID>): List<Device>

    fun deleteAllByUserId(userId: UUID)

    fun deleteAllByTokenIn(tokens: Collection<String>)
}
