package digdaserver.global.infra.filter

import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.jwt.util.JWTUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

class DigdaJWTFilter(
    private val jwtUtil: JWTUtil,
    private val excludedPaths: List<String>
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(DigdaJWTFilter::class.java)

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestURI = request.requestURI
        val method = request.method

        // 🔒 소셜 로그인 요청에 대한 중복 로그인 체크
        if ((requestURI == "/auth/login" && method == "POST") ||
            (requestURI.contains("/api/oauth2/login") && method == "POST") ||
            (requestURI.contains("/api/oauth2/callback"))
        ) {
            val accessToken = jwtUtil.getAccessTokenFromHeaders(request)

            if (accessToken != null && jwtUtil.jwtVerify(accessToken, "access")) {
                throw DigdaException(ErrorCode.DUPLICATE_LOGIN)
            }

            filterChain.doFilter(request, response)
            return
        }

        // 🔑 Access Token 추출
        val accessToken = jwtUtil.getAccessTokenFromHeaders(request)

        log.debug("Access Token: {}", accessToken)

        // 토큰 없거나 undefined/null 문자열일 경우 → 인증 없이 통과
        if (accessToken.isNullOrBlank() ||
            accessToken == "undefined" ||
            accessToken == "null"
        ) {
            filterChain.doFilter(request, response)
            return
        }

        // 🔐 토큰 검증 실패
        if (!jwtUtil.jwtVerify(accessToken, "access")) {
            throw DigdaException(ErrorCode.ACCESS_TOKEN_INVALID)
        }

        // 토큰 유효 → ID / Role 추출
        val userId = jwtUtil.getId(accessToken)
        log.debug("User ID: {}", userId)

        val role = jwtUtil.getRole(accessToken)
        val authority: GrantedAuthority = SimpleGrantedAuthority(role.key)

        log.debug("User Role: {}", role.key)

        // SecurityContext 인증 설정
        val authentication =
            UsernamePasswordAuthenticationToken(userId, null, listOf(authority))

        SecurityContextHolder.getContext().authentication = authentication
        log.debug("인증 설정 완료: {}", SecurityContextHolder.getContext().authentication)

        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val matcher = AntPathMatcher()
        return excludedPaths.any { pattern ->
            matcher.match(pattern, request.servletPath)
        }
    }
}
