package digdaserver.global.infra.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 모든 API 요청 진입에 대해 한 줄로 표준 로그를 남긴다.
 *
 * - JWT 필터 뒤에 체이닝되어 SecurityContext 의 userId 를 함께 기록
 * - 헬스체크/Swagger/static 등 노이즈 경로는 스킵
 * - 응답 종료 시점에 method, path, userId, status, latency 한 줄 로그
 *
 * "내가 분명히 API 진입점마다 로그 박으라고 했지" 라는 운영 요구에 대한
 * 컨트롤러별 일일이 추가하지 않는 글로벌 솔루션.
 */
class ApiAccessLogFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(ApiAccessLogFilter::class.java)

    /** 로그에서 제외할 prefix — 헬스체크/문서/정적자원. */
    private val skipPrefixes = listOf(
        "/api/healthcheck",
        "/actuator",
        "/v3/api-docs",
        "/swagger-ui",
        "/favicon.ico"
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val uri = request.requestURI
        if (skipPrefixes.any { uri.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val start = System.currentTimeMillis()
        try {
            filterChain.doFilter(request, response)
        } finally {
            val latency = System.currentTimeMillis() - start
            val userId = resolveUserId()
            val query = request.queryString?.let { "?$it" } ?: ""
            log.info(
                "action=API 진입, method={}, path={}{}, userId={}, status={}, latency={}ms",
                request.method,
                uri,
                query,
                userId,
                response.status,
                latency
            )
        }
    }

    private fun resolveUserId(): String {
        val auth = SecurityContextHolder.getContext().authentication ?: return "-"
        val principal = auth.principal?.toString()?.takeIf { it.isNotBlank() }
            ?: auth.name?.takeIf { it.isNotBlank() }
            ?: return "-"
        return if (principal == "anonymousUser") "-" else principal
    }
}
