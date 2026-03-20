package digdaserver.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import digdaserver.global.infra.exception.auth.DigdaAuthExceptionFilter
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
        "/api/app/reissue",
        "/api/oauth2/login/**",
        "/api/oauth2/callback/**",
        "/api/healthcheck",
        "/api/admin/**",
        "/api/test/**",
        "/actuator/**",
        "/api/callback/**"
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
                    allowedOriginPatterns = listOf("https://history.netlify.app")
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
                .requestMatchers("/api/oauth2/login/**").permitAll()
                .requestMatchers("/api/oauth2/callback/**", "/api/test/**", "/api/callback/**").permitAll()
                .requestMatchers("/api/admin/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/favicon.ico", "/api/region").permitAll()
                .requestMatchers("/api/app/reissue", "/api/web/reissue").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/app/public/**", "/api/web/public/**").permitAll()
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

        return http.build()
    }
}
