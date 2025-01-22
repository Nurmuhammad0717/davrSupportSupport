//package uz.davrmobile.support.controller.chat
//
//import org.springframework.data.domain.Page
//import org.springframework.data.domain.Pageable
//import org.springframework.web.bind.annotation.GetMapping
//import org.springframework.web.bind.annotation.PathVariable
//import org.springframework.web.bind.annotation.PostMapping
//import org.springframework.web.bind.annotation.PutMapping
//import org.springframework.web.bind.annotation.RequestBody
//import org.springframework.web.bind.annotation.RequestParam
//import org.springframework.web.bind.annotation.RestController
//import uz.davrmobile.support.dto.BaseResult
//import uz.davrmobile.support.dto.IsCloseDto
//import uz.davrmobile.support.dto.MessageDto
//import uz.davrmobile.support.dto.MessageModel
//import uz.davrmobile.support.dto.ResultUnboundMessagesDto
//import uz.davrmobile.support.dto.SectionDto
//import uz.davrmobile.support.enm.AppealSection
//import uz.davrmobile.support.usecase.AskToCloseChatUseCase
//import uz.davrmobile.support.usecase.BindChatToMeUseCase
//import uz.davrmobile.support.usecase.GetAllSectionsUseCase
//import uz.davrmobile.support.usecase.GetChatsUseCase
//import uz.davrmobile.support.usecase.GetUnboundChatsUseCase
//import uz.davrmobile.support.usecase.IsClosedUseCase
//import uz.davrmobile.support.usecase.SendMessageUseCase
//import uz.davrmobile.support.util.IsModerator
//
//@RestController
//class ChatModeratorController(
//    private val getAllSectionsUseCase: GetAllSectionsUseCase,
//    private val getUnboundChatsUseCase: GetUnboundChatsUseCase,
//    private val bindChatToMeUseCase: BindChatToMeUseCase,
//    private val sendMessageUseCase: SendMessageUseCase,
//    private val getChatsUseCase: GetChatsUseCase,
//    private val askToCloseChatUseCase: AskToCloseChatUseCase,
//    private val isClosedUseCase: IsClosedUseCase,
//) {
//
//    @GetMapping("/section")
//    fun getSectionList(): List<SectionDto> = getAllSectionsUseCase.execute()
//
//    @GetMapping("/chats")
//    fun getChats(pageable: Pageable): Any = getChatsUseCase.execute(pageable)
//
//    @IsModerator
//    @GetMapping("/unbound")
//    fun getUnboundChats(
//        @RequestParam sections: List<AppealSection>?,
//        pageable: Pageable
//    ): Page<ResultUnboundMessagesDto> {
//        return getUnboundChatsUseCase.execute(sections, pageable)
//    }
//
//    @IsModerator
//    @PutMapping("/bind/{chatUid}")
//    fun bindToMe(@PathVariable chatUid: String): BaseResult {
//        return bindChatToMeUseCase.execute(chatUid)
//    }
//
//    @PostMapping("/{chatId}")
//    fun sendMessage(@PathVariable chatId: String, @RequestBody dto: MessageDto): MessageModel {
//        return sendMessageUseCase.execute(chatId, dto)
//    }
//
//    @PostMapping("/ask-close/{chatId}")
//    fun askForCloseChat(@PathVariable chatId: String): BaseResult {
//        return askToCloseChatUseCase.execute(chatId)
//    }
//
//    @PutMapping("/is-close/{chatId}")
//    fun isClose(@PathVariable chatId: String, @RequestBody dto: IsCloseDto): BaseResult {
//        return isClosedUseCase.execute(chatId, dto)
//    }
//}
