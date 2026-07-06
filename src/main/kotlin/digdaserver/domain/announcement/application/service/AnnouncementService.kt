package digdaserver.domain.announcement.application.service

import digdaserver.domain.announcement.domain.entity.Announcement
import digdaserver.domain.announcement.domain.entity.AnnouncementTarget
import org.springframework.data.domain.Page
import java.util.UUID

interface AnnouncementService {

    fun send(
        target: AnnouncementTarget,
        targetUserIds: List<UUID>?,
        title: String,
        body: String
    ): Announcement

    fun search(keyword: String?, page: Int, size: Int): Page<Announcement>
}
