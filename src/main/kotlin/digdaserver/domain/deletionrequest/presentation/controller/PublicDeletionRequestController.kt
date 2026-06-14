package digdaserver.domain.deletionrequest.presentation.controller

import digdaserver.domain.deletionrequest.application.service.DeletionRequestService
import digdaserver.domain.deletionrequest.presentation.dto.req.CreateAccountDeletionRequest
import digdaserver.domain.deletionrequest.presentation.dto.req.CreateDataDeletionRequest
import digdaserver.domain.deletionrequest.presentation.dto.res.DeletionRequestResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 비로그인 공개 삭제 요청 API. 디그팟 어드민 웹의 공개 페이지에서 호출한다.
 * 경로 api/web/public 하위는 SecurityConfig 에서 인증 없이 허용된다.
 */
@RestController
@RequestMapping("/api/web/public/deletion-requests")
@Tag(name = "Public - DeletionRequest", description = "비로그인 계정/데이터 삭제 요청 API")
class PublicDeletionRequestController(
    private val deletionRequestService: DeletionRequestService
) {

    @Operation(summary = "계정 삭제 요청", description = "가입 이메일로 계정 전체 삭제를 요청합니다.")
    @PostMapping("/account")
    fun account(
        @RequestBody @Valid
        request: CreateAccountDeletionRequest
    ): ResponseEntity<DeletionRequestResponse> {
        val response = deletionRequestService.createAccountDeletion(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "데이터 삭제 요청", description = "그룹방·데이터를 지정해 일부 데이터 삭제를 요청합니다.")
    @PostMapping("/data")
    fun data(
        @RequestBody @Valid
        request: CreateDataDeletionRequest
    ): ResponseEntity<DeletionRequestResponse> {
        val response = deletionRequestService.createDataDeletion(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
