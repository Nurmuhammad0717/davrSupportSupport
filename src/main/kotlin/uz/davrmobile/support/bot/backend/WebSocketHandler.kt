package uz.likwer.zeroonetask4supportbot.bot.backend

import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

class WebSocketHandler : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload
        println("Received: $payload")
        session.sendMessage(TextMessage("Echo: $payload"))
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        println("Disconnected: ${session.id}")
    }
}
