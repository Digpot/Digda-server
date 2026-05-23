package digdaserver.domain.upload.domain.repository

import digdaserver.domain.upload.domain.entity.UploadedImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UploadedImageRepository : JpaRepository<UploadedImage, Long> {

    @Modifying
    @Query("DELETE FROM UploadedImage ui WHERE ui.user.id = :userId")
    fun deleteAllByUserId(@Param("userId") userId: UUID)
}
