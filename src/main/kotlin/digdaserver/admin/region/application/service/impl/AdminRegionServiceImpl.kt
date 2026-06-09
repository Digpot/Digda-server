package digdaserver.admin.region.application.service.impl

import digdaserver.admin.region.application.service.AdminRegionService
import digdaserver.domain.diary.domain.entity.GroupRegionFill
import digdaserver.domain.diary.domain.repository.GroupRegionFillRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminRegionServiceImpl(
    private val groupRegionFillRepository: GroupRegionFillRepository
) : AdminRegionService {

    override fun filled(groupRoomId: Long): List<String> =
        groupRegionFillRepository.findAllByGroupRoomId(groupRoomId).map { it.regionKey }

    @Transactional
    override fun fill(groupRoomId: Long, regionKeys: List<String>): List<String> {
        for (key in regionKeys) {
            val k = key.trim()
            if (k.isEmpty()) continue
            if (!groupRegionFillRepository.existsByGroupRoomIdAndRegionKey(groupRoomId, k)) {
                groupRegionFillRepository.save(GroupRegionFill(groupRoomId = groupRoomId, regionKey = k))
            }
        }
        return filled(groupRoomId)
    }

    @Transactional
    override fun unfill(groupRoomId: Long, regionKeys: List<String>): List<String> {
        for (key in regionKeys) {
            groupRegionFillRepository.deleteByGroupRoomIdAndRegionKey(groupRoomId, key.trim())
        }
        return filled(groupRoomId)
    }

    @Transactional
    override fun clear(groupRoomId: Long) {
        groupRegionFillRepository.deleteByGroupRoomId(groupRoomId)
    }
}
