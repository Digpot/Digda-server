package digdaserver.global.infra.exception

import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.exception.error.response.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(DigdaException::class)
    fun handleDigdaException(
        e: DigdaException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val ctx = requestContext(request)
        // 4xx 비즈니스 에러는 warn, 5xx 만 error 로 분기해 운영 로그 노이즈 감소.
        if (e.httpStatusCode >= 500) {
            log.error(
                "DigdaException(5xx) {}, code={}, message={}",
                ctx,
                e.errorCode.code,
                e.message,
                e
            )
        } else {
            log.warn(
                "DigdaException {}, status={}, code={}, message={}",
                ctx,
                e.httpStatusCode,
                e.errorCode.code,
                e.message
            )
        }

        return ResponseEntity
            .status(e.httpStatusCode)
            .body(ErrorResponse.of(e))
    }

    /**
     * Spring multipart 가 application yml 의 max-file-size / max-request-size 한도를
     * 넘는 업로드를 거부할 때 던지는 예외. 기본 Spring 응답은 영문 "Request Entity Too Large"
     * 라 사용자에게 그대로 노출되면 의미 전달 안 됨. ErrorCode.FILE_TOO_LARGE 의 한글
     * 메시지로 통일해서 413 으로 응답.
     */
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(
        e: MaxUploadSizeExceededException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn(
            "업로드 크기 초과 {}, maxUploadSize={}, message={}",
            requestContext(request),
            e.maxUploadSize,
            e.message
        )
        return ResponseEntity
            .status(ErrorCode.FILE_TOO_LARGE.httpCode)
            .body(ErrorResponse.of(ErrorCode.FILE_TOO_LARGE))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(
        e: NoResourceFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("404 Not Found {}, message={}", requestContext(request), e.message)
        return ResponseEntity
            .status(404)
            .body(ErrorResponse.of(ErrorCode.SERVER_ERROR))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error(
            "Unexpected exception {}, message={}",
            requestContext(request),
            e.message,
            e
        )

        return ResponseEntity
            .status(500)
            .body(ErrorResponse.of(ErrorCode.SERVER_ERROR))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleJsonParseError(
        e: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val root = e.rootCause

        return when (root) {
            is DigdaException -> {
                log.warn(
                    "JSON parse → DigdaException {}, code={}, message={}",
                    requestContext(request),
                    root.errorCode.code,
                    root.message
                )
                ResponseEntity
                    .status(root.httpStatusCode)
                    .body(ErrorResponse.of(root))
            }

            else -> {
                log.warn(
                    "JSON parse 실패 {}, message={}",
                    requestContext(request),
                    root?.message ?: e.message
                )
                ResponseEntity
                    .status(400)
                    .body(
                        ErrorResponse.of(
                            ErrorCode.PARAMETER_GRAMMAR_ERROR,
                            root?.message ?: "잘못된 요청입니다."
                        )
                    )
            }
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "잘못된 값입니다.")
        }
        log.warn(
            "요청 검증 실패 {}, details={}",
            requestContext(request),
            details
        )

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(ErrorCode.PARAMETER_VALIDATION_ERROR, details))
    }

    /**
     * 모든 예외 로그에 공통으로 붙는 요청 컨텍스트. ApiAccessLogFilter 와 키 포맷을
     * 맞춰 같은 한 트랜잭션을 grep 으로 묶을 수 있게 한다.
     */
    private fun requestContext(request: HttpServletRequest): String {
        val userId = SecurityContextHolder.getContext().authentication?.principal
            ?.toString()
            ?.takeIf { it.isNotBlank() && it != "anonymousUser" }
            ?: "-"
        val query = request.queryString?.let { "?$it" } ?: ""
        return "method=${request.method}, path=${request.requestURI}$query, userId=$userId"
    }
}
