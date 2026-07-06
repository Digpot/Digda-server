package digdaserver.admin.db.presentation.controller

import digdaserver.admin.db.application.service.AdminDbService
import digdaserver.admin.db.presentation.dto.req.AdminUpsertRowRequest
import digdaserver.admin.db.presentation.dto.res.AdminColumnInfoResponse
import digdaserver.admin.db.presentation.dto.res.AdminTableInfoResponse
import digdaserver.admin.db.presentation.dto.res.AdminTableRowsResponse
import digdaserver.admin.db.presentation.dto.res.AdminWriteResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/db")
@Tag(name = "Admin - DB", description = "관리자 DB 메타/데이터 조회·수정 API")
class AdminDbController(
    private val adminDbService: AdminDbService
) {

    @Operation(summary = "전체 테이블 목록 조회")
    @GetMapping("/tables")
    fun listTables(): ResponseEntity<List<AdminTableInfoResponse>> =
        ResponseEntity.ok(adminDbService.listTables())

    @Operation(summary = "테이블 컬럼 정보 조회")
    @GetMapping("/tables/{name}/columns")
    fun listColumns(@PathVariable name: String): ResponseEntity<List<AdminColumnInfoResponse>> =
        ResponseEntity.ok(adminDbService.listColumns(name))

    @Operation(summary = "테이블 데이터 조회", description = "페이징·정렬. size 최대 200.")
    @GetMapping("/tables/{name}/rows")
    fun readRows(
        @PathVariable name: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) orderBy: String?,
        @RequestParam(required = false) direction: String?
    ): ResponseEntity<AdminTableRowsResponse> =
        ResponseEntity.ok(adminDbService.readRows(name, page, size, orderBy, direction))

    @Operation(summary = "행 추가", description = "values 맵의 컬럼만 INSERT.")
    @PostMapping("/tables/{name}/rows")
    fun insertRow(
        @PathVariable name: String,
        @RequestBody request: AdminUpsertRowRequest
    ): ResponseEntity<AdminWriteResultResponse> {
        val affected = adminDbService.insertRow(name, request.values)
        return ResponseEntity.ok(AdminWriteResultResponse(affected = affected))
    }

    @Operation(
        summary = "행 수정",
        description = "PK로만 매칭. PK 컬럼은 변경 불가, values에 PK 포함되어도 무시됨. 1행 영향 강제."
    )
    @PatchMapping("/tables/{name}/rows")
    fun updateRow(
        @PathVariable name: String,
        @RequestParam pk: Map<String, String>,
        @RequestBody request: AdminUpsertRowRequest
    ): ResponseEntity<AdminWriteResultResponse> {
        val affected = adminDbService.updateRow(name, pk, request.values)
        return ResponseEntity.ok(AdminWriteResultResponse(affected = affected))
    }

    @Operation(
        summary = "행 삭제",
        description = "PK로만 매칭. WHERE 없는 전체 삭제 불가. 1행 영향 강제."
    )
    @DeleteMapping("/tables/{name}/rows")
    fun deleteRow(
        @PathVariable name: String,
        @RequestParam pk: Map<String, String>
    ): ResponseEntity<AdminWriteResultResponse> {
        val affected = adminDbService.deleteRow(name, pk)
        return ResponseEntity.ok(AdminWriteResultResponse(affected = affected))
    }
}
