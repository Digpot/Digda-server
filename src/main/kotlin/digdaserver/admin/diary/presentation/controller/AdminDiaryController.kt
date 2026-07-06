package digdaserver.admin.diary.presentation.controller

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.diary.application.service.AdminDiaryService
import digdaserver.admin.diary.presentation.dto.res.AdminDiaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/diaries")
@Tag(name = "Admin - Diary", description = "관리자 일기 관리 API")
class AdminDiaryController(
    private val adminDiaryService: AdminDiaryService
) {

    @Operation(summary = "일기 목록 조회", description = "페이징, 키워드(제목/내용)")
    @GetMapping
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminDiaryResponse>> {
        return ResponseEntity.ok(adminDiaryService.search(keyword, page, size))
    }

    @Operation(summary = "일기 상세 조회")
    @GetMapping("/{diaryId}")
    fun getDetail(@PathVariable diaryId: Long): ResponseEntity<AdminDiaryResponse> {
        return ResponseEntity.ok(adminDiaryService.getDetail(diaryId))
    }

    @Operation(summary = "일기 삭제", description = "관리자 권한으로 일기를 영구 삭제합니다.")
    @DeleteMapping("/{diaryId}")
    fun delete(@PathVariable diaryId: Long): ResponseEntity<Void> {
        adminDiaryService.delete(diaryId)
        return ResponseEntity.noContent().build()
    }
}
