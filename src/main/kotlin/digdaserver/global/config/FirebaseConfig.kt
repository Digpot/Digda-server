package digdaserver.global.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FirebaseConfig {

    @Value("\${fcm.project-id}")
    private lateinit var projectId: String

    @Value("\${fcm.private-key-id}")
    private lateinit var privateKeyId: String

    @Value("\${fcm.private-key}")
    private lateinit var privateKey: String

    @Value("\${fcm.client-email}")
    private lateinit var clientEmail: String

    @Value("\${fcm.client-id}")
    private lateinit var clientId: String

    @PostConstruct
    fun initialize() {
        if (FirebaseApp.getApps().isNotEmpty()) return

        val json = """
            {
              "type": "service_account",
              "project_id": "$projectId",
              "private_key_id": "$privateKeyId",
              "private_key": "${privateKey.replace("\\n", "\n")}",
              "client_email": "$clientEmail",
              "client_id": "$clientId",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token",
              "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs"
            }
        """.trimIndent()

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(json.byteInputStream()))
            .build()

        FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}
