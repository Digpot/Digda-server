package digdaserver.admin.schedule.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.schedule.application.service.AdminScheduleService
import digdaserver.admin.schedule.presentation.dto.res.AdminScheduleResponse
import digdaserver.domain.schedule.domain.repository.ScheduleRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminScheduleServiceImpl(
    private val scheduleRepository: ScheduleRepository
) : AdminScheduleService {

    override fun search(keyword: String?, page: Int, size: Int): AdminPageResponse<AdminScheduleResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = scheduleRepository.searchForAdmin(keyword, pageable)
        return AdminPageResponse.of(result, AdminScheduleResponse::from)
    }

    override fun getDetail(scheduleId: Long): AdminScheduleResponse {
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { DigdaException(ErrorCode.SCHEDULE_NOT_FOUND) }
        return AdminScheduleResponse.from(schedule)
    }
}
