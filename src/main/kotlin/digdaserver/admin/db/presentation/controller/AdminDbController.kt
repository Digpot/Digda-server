package digdaserver.admin.db.presentation.controller

import digdaserver.admin.db.application.service.AdminDbService
import digdaserver.admin.db.presentation.dto.res.AdminColumnInfoResponse
import digdaserver.admin.db.presentation.dto.res.AdminTableInfoResponse
import digdaserver.admin.db.presentation.dto.res.AdminTableRowsResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/db")
@Tag(name = "Admin - DB", description = "관리자 DB 메타/데이터 조회 API")
class AdminDbController(
    private val adminDbService: AdminDbService
) {

    @Operation(summary = "전체 테이블 목록 조회", description = "현재 스키마의 BASE TABLE 목록을 반환합니다.")
    @GetMapping("/tables")
    fun listTables(): ResponseEntity<List<AdminTableInfoResponse>> {
        return ResponseEntity.ok(adminDbService.listTables())
    }

    @Operation(summary = "테이블 컬럼 정보 조회")
    @GetMapping("/tables/{name}/columns")
    fun listColumns(
        @PathVariable name: String
    ): ResponseEntity<List<AdminColumnInfoResponse>> {
        return ResponseEntity.ok(adminDbService.listColumns(name))
    }

    @Operation(summary = "테이블 데이터 조회", description = "페이징 및 컬럼 기반 데이터 반환. size는 최대 200.")
    @GetMapping("/tables/{name}/rows")
    fun readRows(
        @PathVariable name: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) orderBy: String?,
        @RequestParam(required = false) direction: String?
    ): ResponseEntity<AdminTableRowsResponse> {
        return ResponseEntity.ok(adminDbService.readRows(name, page, size, orderBy, direction))
    }
}
