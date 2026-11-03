package digdaserver.global.infra.security

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 비밀번호 대입 방어 — 실패 횟수를 Redis 에 세고, 한도를 넘으면 잠근다.
 *
 * 키는 호출부가 정한다(`admin-login:{email}`, `pii-reveal:{adminId}`). 실패할 때마다 창이
 * 처음부터 다시 열리므로(EXPIRE 갱신) 잠긴 동안 계속 두드리면 잠금이 계속 연장된다.
 * Redis 가 죽었을 때는 막지 않는다(fail-open) — 관리자 전원이 잠기는 쪽이 더 큰 장애다.
 */
@Component
class AttemptLimiter(
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isLocked(key: String, maxAttempts: Int): Boolean =
        runCatching { (redisTemplate.opsForValue().get(prefixed(key))?.toIntOrNull() ?: 0) >= maxAttempts }
            .onFailure { log.warn("[AttemptLimiter] Redis 조회 실패 — 제한 없이 통과: {}", it.message) }
            .getOrDefault(false)

    fun recordFailure(key: String, window: Duration) {
        runCatching {
            val k = prefixed(key)
            redisTemplate.opsForValue().increment(k)
            redisTemplate.expire(k, window)
        }.onFailure { log.warn("[AttemptLimiter] Redis 기록 실패: {}", it.message) }
    }

    fun reset(key: String) {
        runCatching { redisTemplate.delete(prefixed(key)) }
    }

    private fun prefixed(key: String) = "attempt:$key"
}
