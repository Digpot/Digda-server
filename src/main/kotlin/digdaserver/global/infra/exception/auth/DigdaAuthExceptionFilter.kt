package digdaserver.global.infra.exception.auth

import com.fasterxml.jackson.databind.ObjectMapper
import digdaserver.global.infra.exception.error.ErrorCode
<<<<<<<< HEAD:src/main/kotlin/digdaserver/global/infra/exception/auth/DigdaAuthExceptionFilter.kt
import digdaserver.global.infra.exception.error.DigdaException
========
import digdaserver.global.infra.exception.error.DigdaServerException
>>>>>>>> origin/dev:src/main/kotlin/digdaserver/global/infra/exception/auth/DigdaServerExceptionFilter.kt
import digdaserver.global.infra.exception.error.response.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter
import javax.security.sasl.AuthenticationException

<<<<<<<< HEAD:src/main/kotlin/digdaserver/global/infra/exception/auth/DigdaAuthExceptionFilter.kt
class DigdaAuthExceptionFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(DigdaAuthExceptionFilter::class.java)
========
class DigdaServerExceptionFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(DigdaServerExceptionFilter::class.java)
>>>>>>>> origin/dev:src/main/kotlin/digdaserver/global/infra/exception/auth/DigdaServerExceptionFilter.kt

    @Throws(ServletException::class, java.io.IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request, response)
<<<<<<<< HEAD:src/main/kotlin/digdaserver/global/infra/exception/auth/DigdaAuthExceptionFilter.kt
        } catch (e: DigdaException) {
========
        } catch (e: DigdaServerException) {
>>>>>>>> origin/dev:src/main/kotlin/digdaserver/global/infra/exception/auth/DigdaServerExceptionFilter.kt
            handleFlowException(response, e)
        } catch (e: AuthenticationException) {
            handleAuthenticationException(response)
        } catch (e: Exception) {
            log.error("Filter에서 예상치 못한 오류 발생", e)
            handleUnexpectedException(response)
        }
    }

    @Throws(java.io.IOException::class)
<<<<<<<< HEAD:src/main/kotlin/digdaserver/global/infra/exception/auth/DigdaAuthExceptionFilter.kt
    private fun handleFlowException(response: HttpServletResponse, e: DigdaException) {
========
    private fun handleFlowException(response: HttpServletResponse, e: DigdaServerException) {
>>>>>>>> origin/dev:src/main/kotlin/digdaserver/global/infra/exception/auth/DigdaServerExceptionFilter.kt
        log.error(
            "Filter에서 DigdaException 발생 - ErrorCode: {}, Message: {}",
            e.errorCode,
            e.message
        )

        response.status = e.httpStatusCode
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"

        val errorResponse = ErrorResponse.of(e)
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }

    @Throws(java.io.IOException::class)
    private fun handleAuthenticationException(response: HttpServletResponse) {
        log.error("Filter에서 AuthenticationException 발생")

        response.status = 401
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"

        val errorResponse = ErrorResponse.of(ErrorCode.UNAUTHORIZED)
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }

    @Throws(java.io.IOException::class)
    private fun handleUnexpectedException(response: HttpServletResponse) {
        response.status = 500
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"

        val errorResponse = ErrorResponse.of(ErrorCode.SERVER_ERROR)
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
