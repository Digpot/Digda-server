package digdaserver.domain.notification.application.service

import digdaserver.domain.notification.presentation.dto.res.NotificationListResponse
import java.util.UUID

interface NotificationService {

    fun getNotifications(userId: UUID, limit: Int, offset: Int): NotificationListResponse

    fun markAsRead(userId: UUID, notificationId: Long, isRead: Boolean)

    fun markAllAsRead(userId: UUID)

    fun deleteNotification(userId: UUID, notificationId: Long)

    fun notifyGroupRoomDeleteScheduled(groupRoomId: Long, actorUserId: UUID)

    fun notifyMemberJoined(groupRoomId: Long, joinedUserId: UUID)

    fun notifyDiaryWritten(groupRoomId: Long, diaryId: Long, authorUserId: UUID, diaryTitle: String)

    fun notifyScheduleCreated(
        groupRoomId: Long,
        scheduleId: Long,
        creatorUserId: UUID,
        scheduleTitle: String,
        participantUserIds: List<UUID>
    )

    fun notifyMemberLeft(groupRoomId: Long, leaverUserId: UUID)

    fun notifyOwnershipTransferred(groupRoomId: Long, actorUserId: UUID, newOwnerUserId: UUID)

    fun notifyScheduleParticipantsAdded(
        groupRoomId: Long,
        scheduleId: Long,
        actorUserId: UUID,
        scheduleTitle: String,
        addedParticipantUserIds: List<UUID>
    )

    fun notifyCommentOnSchedule(
        groupRoomId: Long,
        scheduleId: Long,
        commenterUserId: UUID,
        scheduleTitle: String
    )

    fun notifyCommentOnDiary(
        groupRoomId: Long,
        diaryId: Long,
        commenterUserId: UUID,
        diaryTitle: String
    )

    fun notifyMemberRemoved(groupRoomId: Long, actorUserId: UUID, removedUserId: UUID)

    fun sendAnnouncement(
        targetUserIds: List<UUID>?,
        title: String,
        body: String
    ): Int
}
