package digdaserver.admin.schedule.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.schedule.presentation.dto.res.AdminScheduleResponse

interface AdminScheduleService {

    fun search(keyword: String?, page: Int, size: Int): AdminPageResponse<AdminScheduleResponse>

    fun getDetail(scheduleId: Long): AdminScheduleResponse
}
