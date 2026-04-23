package digdaserver.admin.dashboard.application.service

import digdaserver.admin.dashboard.presentation.dto.res.DashboardSummaryResponse

interface AdminDashboardService {

    fun getSummary(): DashboardSummaryResponse
}
