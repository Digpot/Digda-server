package digdaserver.global.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "디그다 API 명세서",
        description = "API 명세서",
        version = "v1",
        contact = Contact(
            name = "최승호",
            email = "chltmdgh517@naver.com"
        )
    )
)
class SwaggerConfig
