package digdaserver.global.infra.exception

import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.exception.error.response.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(DigdaException::class)
    fun handleDigdaException(e: DigdaException): ResponseEntity<ErrorResponse> {
        log.error("DigdaException - code: {}, message: {}", e.errorCode.code, e.message)

        return ResponseEntity
            .status(e.httpStatusCode)
            .body(ErrorResponse.of(e))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unexpected exception", e)

        return ResponseEntity
            .status(500)
            .body(ErrorResponse.of(ErrorCode.SERVER_ERROR))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleJsonParseError(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        val root = e.rootCause

        return when (root) {
            is DigdaException ->
                ResponseEntity
                    .status(root.httpStatusCode)
                    .body(ErrorResponse.of(root))

            else ->
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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "잘못된 값입니다.")
        }

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(ErrorCode.PARAMETER_VALIDATION_ERROR, details))
    }
}
