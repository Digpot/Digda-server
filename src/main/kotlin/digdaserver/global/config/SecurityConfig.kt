package digdaserver.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import digdaserver.global.infra.exception.auth.DigdaAuthExceptionFilter
import digdaserver.global.infra.filter.ApiAccessLogFilter
import digdaserver.global.infra.filter.DigdaJWTFilter
import digdaserver.global.jwt.util.JWTUtil
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.filter.CorsFilter

@Configuration
@EnableWebSecurity(debug = false)
class SecurityConfig(
    private val jwtUtil: JWTUtil,
    private val objectMapper: ObjectMapper
) {

    private val excludedUrls = listOf(
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/favicon.ico",
        "/auth/login",
        "/auth/refresh",
        "/api/app/reissue",
        "/api/oauth2/login/**",
        "/api/oauth2/callback/**",
        "/api/healthcheck",
        "/api/admin/auth/login",
        // WARNING: /api/test/** is permitted without authentication.
        // This path must NOT be exposed in production. Restrict or remove before prod deploy.
        "/api/test/**",
        "/actuator/**",
        "/api/callback/**",
        "/api/test/oauth2/login/**",
        // WebSocket 핸드셰이크 — 인증은 STOMP CONNECT 프레임의 토큰으로 수행
        "/ws/**"
    )

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
        http.formLogin { it.disable() }
        http.httpBasic { it.disable() }

        http.cors { cors ->
            cors.configurationSource {
                CorsConfiguration().apply {
                    allowedOriginPatterns = listOf(
                        "https://history.netlify.app",
                        "https://digda-admin.vercel.app",
                        "http://localhost:5173",
                        "http://localhost:3000"
                    )
                    allowedMethods = listOf("*")
                    allowedHeaders = listOf("*")
                    exposedHeaders = listOf(HttpHeaders.AUTHORIZATION)
                    allowCredentials = true
                    maxAge = 3600L
                }
            }
        }

        http.sessionManagement {
            it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }

        http.exceptionHandling { handler ->
            handler.authenticationEntryPoint { _, response, _ ->
                response.sendError(401, "Unauthorized")
            }
        }

        http.authorizeHttpRequests { auth ->
            auth
                .requestMatchers("/api/healthcheck").permitAll()
                .requestMatchers("/auth/login", "/auth/refresh").permitAll()
                .requestMatchers("/api/oauth2/login/**").permitAll()
                // WARNING: /api/test/** is open without authentication — dev-only endpoint, must not reach production
                .requestMatchers("/api/oauth2/callback/**", "/api/test/**", "/api/callback/**").permitAll()
                .requestMatchers("/api/admin/auth/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/favicon.ico", "/api/region").permitAll()
                .requestMatchers("/api/app/reissue", "/api/web/reissue").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/app/public/**", "/api/web/public/**", "/api/test/oauth2/login/**").permitAll()
                // WebSocket 핸드셰이크 — 이후 STOMP CONNECT 에서 JWT 검증
                .requestMatchers("/ws/**").permitAll()
                .anyRequest().authenticated()
        }

        http.addFilterAfter(
            DigdaAuthExceptionFilter(objectMapper),
            CorsFilter::class.java
        )

        http.addFilterAfter(
            DigdaJWTFilter(jwtUtil, excludedUrls),
            UsernamePasswordAuthenticationFilter::class.java
        )

        // JWT 인증이 끝난 직후에 진입 로그를 남겨 SecurityContext 의 userId 가
        // 포함되게 한다. 컨트롤러별로 일일이 박지 않고 한 곳에서 보장.
        http.addFilterAfter(
            ApiAccessLogFilter(),
            DigdaJWTFilter::class.java
        )

        return http.build()
    }
}
