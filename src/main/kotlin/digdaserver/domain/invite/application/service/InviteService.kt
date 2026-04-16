package digdaserver.domain.invite.application.service

import digdaserver.domain.invite.presentation.dto.res.InviteCodeResponse
import digdaserver.domain.invite.presentation.dto.res.InviteJoinResponse
import digdaserver.domain.invite.presentation.dto.res.InviteValidateResponse
import java.util.UUID

interface InviteService {

    fun regenerateInviteCode(userId: UUID, groupRoomId: Long): InviteCodeResponse

    fun validateInviteCode(userId: UUID, code: String): InviteValidateResponse

    fun joinByInviteCode(userId: UUID, code: String): InviteJoinResponse
}
