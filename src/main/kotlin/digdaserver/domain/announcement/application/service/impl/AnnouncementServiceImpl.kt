package digdaserver.domain.announcement.application.service.impl

import digdaserver.domain.announcement.application.service.AnnouncementService
import digdaserver.domain.announcement.domain.entity.Announcement
import digdaserver.domain.announcement.domain.entity.AnnouncementTarget
import digdaserver.domain.announcement.domain.repository.AnnouncementRepository
import digdaserver.domain.notification.application.service.NotificationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AnnouncementServiceImpl(
    private val announcementRepository: AnnouncementRepository,
    private val notificationService: NotificationService
) : AnnouncementService {

    @Transactional
    override fun send(
        target: AnnouncementTarget,
        targetUserIds: List<UUID>?,
        title: String,
        body: String
    ): Announcement {
        val resolvedUserIds = if (target == AnnouncementTarget.ALL) null else targetUserIds
        val recipientCount = notificationService.sendAnnouncement(resolvedUserIds, title, body)

        return announcementRepository.save(
            Announcement(
                title = title,
                body = body,
                targetType = target,
                recipientCount = recipientCount
            )
        )
    }

    override fun search(keyword: String?, page: Int, size: Int): Page<Announcement> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return announcementRepository.searchForAdmin(keyword?.takeIf { it.isNotBlank() }, pageable)
    }
}
