package digdaserver.domain.oauth2.infra

import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date

@Component
class AppleJwtUtils(

    @Value("\${oauth2.apple.team-id}")
    private val teamId: String,

    @Value("\${oauth2.apple.key-id}")
    private val keyId: String,

    @Value("\${oauth2.apple.private-key}")
    private val privateKey: String,

    @Value("\${oauth2.apple.client-id}")
    private val clientId: String
) {

    private val log = LoggerFactory.getLogger(AppleJwtUtils::class.java)

    fun generateClientSecret(): String {
        return try {
            val pKey = getPrivateKey()

            Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setHeaderParam("alg", "ES256")
                .setIssuer(teamId)
                .setIssuedAt(Date())
                .setExpiration(Date(System.currentTimeMillis() + 3600000)) // 1시간
                .setAudience("https://appleid.apple.com")
                .setSubject(clientId)
                .signWith(pKey, SignatureAlgorithm.ES256)
                .compact()
        } catch (e: Exception) {
            log.error("Apple JWT 생성 실패", e)
            throw DigdaException(ErrorCode.APPLE_JWT_ERROR)
        }
    }

    private fun getPrivateKey(): PrivateKey {
        return try {
            val privateKeyPEM = privateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")

            val keyBytes = Base64.getDecoder().decode(privateKeyPEM)

            val spec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("EC")

            keyFactory.generatePrivate(spec)
        } catch (e: Exception) {
            log.error("Apple Private Key 파싱 실패", e)
            throw DigdaException(ErrorCode.APPLE_KEY_PARSE_ERROR)
        }
    }
}
