package uz.davrmobile.support.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/web-socket-not-use")
            .setAllowedOrigins("*")
            .withSockJS()
            .setSupressCors(true)
            .setClientLibraryUrl("https://cdn.jsdelivr.net/sockjs/1.1.4/sockjs.min.js")
            .setWebSocketEnabled(true)
    }

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/queue")
        config.setApplicationDestinationPrefixes("/app")
    }
}
