package digdaserver.domain.todo.application.service.impl

import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.todo.application.service.TodoService
import digdaserver.domain.todo.domain.entity.Todo
import digdaserver.domain.todo.domain.repository.TodoRepository
import digdaserver.domain.todo.presentation.dto.res.ProgressResponse
import digdaserver.domain.todo.presentation.dto.res.TodoListResponse
import digdaserver.domain.todo.presentation.dto.res.TodoResponse
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class TodoServiceImpl(
    private val todoRepository: TodoRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) : TodoService {

    override fun getTodos(userId: UUID, groupRoomId: Long): TodoListResponse {
        validateGroupRoomMember(groupRoomId, userId)

        val todos = todoRepository.findAllByGroupRoomIdOrdered(groupRoomId)
        val total = todos.size
        val completed = todos.count { it.completed }
        val percent = if (total == 0) 0 else (completed * 100) / total

        return TodoListResponse(
            todos = todos.map { TodoResponse.from(it) },
            progress = ProgressResponse(
                total = total,
                completed = completed,
                percent = percent
            )
        )
    }

    @Transactional
    override fun createTodo(userId: UUID, groupRoomId: Long, text: String): TodoResponse {
        validateGroupRoomMember(groupRoomId, userId)
        validateTodoText(text)

        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val todo = todoRepository.save(
            Todo(
                groupRoom = groupRoom,
                text = text,
                createdBy = user
            )
        )

        return TodoResponse.from(todo)
    }

    @Transactional
    override fun toggleTodo(userId: UUID, groupRoomId: Long, todoId: Long, completed: Boolean): TodoResponse {
        validateGroupRoomMember(groupRoomId, userId)

        val todo = todoRepository.findById(todoId)
            .orElseThrow { DigdaException(ErrorCode.TODO_NOT_FOUND) }

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        todo.toggleComplete(user)

        return TodoResponse.from(todo)
    }

    @Transactional
    override fun deleteTodo(userId: UUID, groupRoomId: Long, todoId: Long) {
        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val todo = todoRepository.findById(todoId)
            .orElseThrow { DigdaException(ErrorCode.TODO_NOT_FOUND) }

        if (todo.createdBy.id != userId && !membership.isOwner) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        todoRepository.delete(todo)
    }

    private fun validateGroupRoomMember(groupRoomId: Long, userId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }
    }

    private fun validateTodoText(text: String) {
        if (text.length > 100) throw DigdaException(ErrorCode.TODO_TEXT_TOO_LONG)
    }
}
