package digdaserver.global.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class S3Config(

    @Value("\${cloud.aws.s3.bucket}")
    private val bucketName: String,

    @Value("\${cloud.aws.region.static}")
    private val bucketRegion: String,

    @Value("\${cloud.aws.credentials.accessKey}")
    private val accessKey: String,

    @Value("\${cloud.aws.credentials.secretKey}")
    private val secretKey: String
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun s3Client(): S3Client {
        log.info("S3 Client 초기화 - region={}, bucket={}", bucketRegion, bucketName)

        val awsCredentials = AwsBasicCredentials.create(accessKey, secretKey)

        return S3Client.builder()
            .region(Region.of(bucketRegion))
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build()
    }
}
