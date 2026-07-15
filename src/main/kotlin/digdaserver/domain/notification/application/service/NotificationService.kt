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

    /** 일정 시작 하루 전 리마인더 — 참가자 및 생성자에게 발송. */
    fun notifyScheduleReminderDayBefore(
        groupRoomId: Long,
        scheduleId: Long,
        scheduleTitle: String,
        recipientUserIds: List<UUID>
    )

    /** 일정 당일 리마인더 — 참가자 및 생성자에게 발송. */
    fun notifyScheduleReminderToday(
        groupRoomId: Long,
        scheduleId: Long,
        scheduleTitle: String,
        recipientUserIds: List<UUID>
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

    fun notifyQuizCreated(
        groupRoomId: Long,
        quizId: Long,
        authorUserId: UUID,
        question: String
    )

    fun notifyQuizAnswered(
        groupRoomId: Long,
        quizId: Long,
        solverUserId: UUID,
        correct: Boolean
    )

    fun notifyMochiLevelUp(
        groupRoomId: Long,
        actorUserId: UUID,
        newLevel: Int,
        stageChanged: Boolean,
        stageName: String
    )

    /** 디코(조력자) 최초 등장. 그룹 멤버 전원에게 1회만 발송. */
    fun notifyDikoUnlocked(groupRoomId: Long, actorUserId: UUID)

    /** 오목 대국 초대 — 초대받은 사람에게만 발송. relatedId=gameId, relatedType=OMOK. */
    fun notifyOmokInvite(
        groupRoomId: Long,
        inviterUserId: UUID,
        inviteeUserId: UUID,
        gameId: Long
    )

    fun sendAnnouncement(
        targetUserIds: List<UUID>?,
        title: String,
        body: String
    ): Int
}
