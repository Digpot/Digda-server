package digdaserver.global.infra.presentation.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class AuthenticationCheckController {

    private val log = LoggerFactory.getLogger(AuthenticationCheckController::class.java)

    @GetMapping("/authcheck")
    @ResponseStatus(HttpStatus.OK)
    fun authCheck() {
        log.debug("Authentication check endpoint accessed")
    }
}
