package digdaserver.global.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import java.io.InputStream

@Configuration
class FirebaseConfig(
    private val resourceLoader: ResourceLoader
) {

    @Value("\${fcm.credentials-path}")
    private lateinit var credentialsPath: String

    @PostConstruct
    fun initialize() {
        if (FirebaseApp.getApps().isNotEmpty()) return

        val stream: InputStream = resourceLoader.getResource(credentialsPath).inputStream

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(stream))
            .build()

        FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}
