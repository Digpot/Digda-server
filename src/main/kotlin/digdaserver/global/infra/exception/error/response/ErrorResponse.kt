package digdaserver.global.infra.exception.error.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode

@JsonInclude(Include.NON_NULL)
data class ErrorResponse(
    val error: ErrorBody
) {

    data class ErrorBody(
        val code: String,
        val message: String,
        val details: Map<String, String>? = null
    )

    companion object {

        fun of(e: DigdaException): ErrorResponse =
            ErrorResponse(
                error = ErrorBody(
                    code = e.errorCode.code,
                    message = e.message ?: e.errorCode.message
                )
            )

        fun of(errorCode: ErrorCode): ErrorResponse =
            ErrorResponse(
                error = ErrorBody(
                    code = errorCode.code,
                    message = errorCode.message
                )
            )

        fun of(errorCode: ErrorCode, customMessage: String): ErrorResponse =
            ErrorResponse(
                error = ErrorBody(
                    code = errorCode.code,
                    message = customMessage
                )
            )

        fun of(errorCode: ErrorCode, details: Map<String, String>): ErrorResponse =
            ErrorResponse(
                error = ErrorBody(
                    code = errorCode.code,
                    message = errorCode.message,
                    details = details
                )
            )
    }
}
