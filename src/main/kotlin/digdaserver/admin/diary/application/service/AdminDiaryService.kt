package digdaserver.admin.diary.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.diary.presentation.dto.res.AdminDiaryResponse

interface AdminDiaryService {

    fun search(keyword: String?, page: Int, size: Int): AdminPageResponse<AdminDiaryResponse>

    fun getDetail(diaryId: Long): AdminDiaryResponse

    fun delete(diaryId: Long)
}
