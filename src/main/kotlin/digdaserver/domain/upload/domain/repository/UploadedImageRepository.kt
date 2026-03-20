package digdaserver.domain.upload.domain.repository

import digdaserver.domain.upload.domain.entity.UploadedImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UploadedImageRepository : JpaRepository<UploadedImage, Long>
