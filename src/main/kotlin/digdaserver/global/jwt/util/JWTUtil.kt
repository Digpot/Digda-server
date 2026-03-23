package digdaserver.global.jwt.util

import digdaserver.domain.user.domain.entity.Role
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Component
class JWTUtil(

    @Value("\${jwt.secret}")
    secret: String,

    @Value("\${jwt.access-expiration}")
    private val accessExpiration: Long,

    @Value("\${jwt.refresh-expiration}")
    private val refreshExpiration: Long
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val secretKey: SecretKey =
        SecretKeySpec(
            secret.toByteArray(StandardCharsets.UTF_8),
            Jwts.SIG.HS256.key().build().algorithm
        )

    fun getId(token: String): String {
        return try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload

            claims.get("id", String::class.java)
        } catch (e: ExpiredJwtException) {
            throw DigdaException(ErrorCode.TOKEN_EXPIRED)
        } catch (e: JwtException) {
            throw DigdaException(ErrorCode.TOKEN_INVALID)
        }
    }

    fun getRole(token: String): Role {
        return try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload

            val value = claims.get("role", String::class.java)
            Role.getByValue("ROLE_$value")
        } catch (e: ExpiredJwtException) {
            throw DigdaException(ErrorCode.TOKEN_EXPIRED)
        } catch (e: JwtException) {
            throw DigdaException(ErrorCode.TOKEN_INVALID)
        }
    }

    fun createAccessToken(id: String, role: Role, email: String): String =
        createJWT(id, role, email, "access", accessExpiration)

    fun createRefreshToken(id: String, role: Role, email: String): String =
        createJWT(id, role, email, "refresh", refreshExpiration)

    private fun createJWT(
        id: String,
        role: Role,
        email: String,
        category: String,
        expiredMS: Long
    ): String {
        return Jwts.builder()
            .claim("category", category)
            .claim("id", id)
            .claim("role", role.toString())
            .claim("email", email)
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(Date(System.currentTimeMillis() + expiredMS))
            .signWith(secretKey)
            .compact()
    }

    fun getAccessTokenFromHeaders(request: HttpServletRequest): String? {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        return header?.replace("Bearer ", "")
    }

    fun jwtVerify(token: String?, type: String): Boolean {
        if (token.isNullOrBlank()) return false

        return try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload

            val category = claims.get("category", String::class.java)
            category == type
        } catch (e: JwtException) {
            log.error("JWT 검증 실패: {}", e.message)
            false
        }
    }
}
