//package uz.davrmobile.support.controller.chat
//
//import org.springframework.data.domain.Page
//import org.springframework.data.domain.Pageable
//import org.springframework.web.bind.annotation.GetMapping
//import org.springframework.web.bind.annotation.PathVariable
//import org.springframework.web.bind.annotation.PostMapping
//import org.springframework.web.bind.annotation.PutMapping
//import org.springframework.web.bind.annotation.RequestBody
//import org.springframework.web.bind.annotation.RestController
//import uz.davrmobile.support.dto.BaseResult
//import uz.davrmobile.support.dto.MessageModel
//import uz.davrmobile.support.dto.RateModel
//import uz.davrmobile.support.model.OpenChatRequest
//import uz.davrmobile.support.model.OpenChatResponse
//import uz.davrmobile.support.usecase.CloseChatUseCase
//import uz.davrmobile.support.usecase.GetChatMessagesUseCase
//import uz.davrmobile.support.usecase.MarkAsReadUseCase
//import uz.davrmobile.support.usecase.OpenChatUseCase
//import uz.davrmobile.support.util.IsUser
//
//@RestController
//class ChatUserController(
//    private val openChatUseCase: OpenChatUseCase,
//    private val markAsReadUseCase: MarkAsReadUseCase,
//    private val getChatMessagesUseCase: GetChatMessagesUseCase,
//    private val closeChatUseCase: CloseChatUseCase,
//) {
//    @IsUser
//    @PostMapping("/open")
//    fun openChat(@RequestBody request: OpenChatRequest): OpenChatResponse = openChatUseCase.execute(request)
//
//    @PutMapping("/read/{chatUid}")
//    fun markAsRead(@PathVariable chatUid: String): BaseResult {
//        return markAsReadUseCase.execute(chatUid)
//    }
//
//    @GetMapping("/{chatId}")
//    fun getChatMessages(@PathVariable chatId: String, pageable: Pageable): Page<MessageModel> =
//        getChatMessagesUseCase.execute(chatId, pageable)
//
//    @IsUser
//    @PutMapping("/close/{chatId}")
//    fun closeChat(@PathVariable chatId: String, @RequestBody rate: RateModel): BaseResult {
//        return closeChatUseCase.execute(chatId, rate)
//    }
//}
