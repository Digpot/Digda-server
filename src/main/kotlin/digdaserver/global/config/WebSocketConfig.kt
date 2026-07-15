package digdaserver.global.config

import digdaserver.global.jwt.util.JWTUtil
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * 오목 실시간 대전용 STOMP WebSocket 설정.
 *
 * - 엔드포인트: `/ws` (HTTP 핸드셰이크는 permitAll — SecurityConfig 참조)
 * - 인증: STOMP CONNECT 프레임의 `Authorization: Bearer <accessToken>` 헤더를
 *   [JWTUtil] 로 검증하고, 세션 Principal(name=userId) 로 설정한다. 이후 프레임은
 *   세션에 묶인 Principal 을 자동으로 갖는다. 토큰이 없거나 무효면 연결 거부.
 * - 브로커: 인메모리 SimpleBroker(`/topic`) — 대국 이벤트는 `/topic/omok/{gameId}`,
 *   클라이언트 발신은 `/app/omok/{gameId}/...` 로 들어온다.
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val jwtUtil: JWTUtil
) : WebSocketMessageBrokerConfigurer {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*")
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
                val accessor = MessageHeaderAccessor.getAccessor(
                    message,
                    StompHeaderAccessor::class.java
                ) ?: return message

                if (StompCommand.CONNECT == accessor.command) {
                    val raw = accessor.getFirstNativeHeader("Authorization")
                    val token = raw?.removePrefix("Bearer ")?.trim()
                    if (token.isNullOrBlank() || !jwtUtil.jwtVerify(token, "access")) {
                        log.warn("action=ws_connect_rejected, reason=invalid_token")
                        return null // CONNECT 거부
                    }
                    val userId = jwtUtil.getId(token)
                    val role = jwtUtil.getRole(token)
                    accessor.user = UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        listOf(SimpleGrantedAuthority(role.key))
                    )
                    log.info("action=ws_connect, userId={}", userId)
                }
                return message
            }
        })
    }
}
