package com.example.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.PublishSubscribeChannel
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.file.dsl.Files
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandler
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

@SpringBootApplication
class ServiceApplication

@Configuration
class WebSocketConfiguration {

    @Bean
    fun filesChannel() = PublishSubscribeChannel()

    @Bean
    fun fileFlow(@Value("\${input-dir:file://\${HOME}/Desktop/in}") inFile: File): IntegrationFlow {

        val autoCreateDirectory = Files
                .inboundAdapter(inFile)
                .autoCreateDirectory(true)

        return IntegrationFlows
                .from(autoCreateDirectory, { pollerConfig -> pollerConfig.poller({ pm -> pm.fixedDelay(1000) }) })
                .channel(this.filesChannel())
                .get()
    }

    @Bean
    fun webSocketHandlerMapping(): HandlerMapping {
        val mapping = SimpleUrlHandlerMapping()
        val channel = filesChannel()

        class ProcessingMessageHandler(val session: WebSocketSession, val sink: FluxSink<WebSocketMessage>) : MessageHandler {

            private val sessionId = session.id
            private val om = ObjectMapper()

            override fun handleMessage(p0: Message<*>) {
                val payload = p0.payload as File
                val updatedPayload = mapOf("sessionId" to sessionId, "path" to payload.absolutePath)
                val jsonPayload = this.om.writeValueAsString(updatedPayload)
                sink.next(session.textMessage(jsonPayload))
            }
        }

        val webSocketHandler = WebSocketHandler { session ->

            val mapOfIntegrations = ConcurrentHashMap<String, ProcessingMessageHandler>()

            val publisher = Flux
                    .create(Consumer<FluxSink<WebSocketMessage>> { sink ->
                        val incomingFiles = ProcessingMessageHandler(session, sink)
                        mapOfIntegrations.put(session.id, incomingFiles)
                        channel.subscribe(incomingFiles)
                    })
                    .doFinally({
                        channel.unsubscribe(mapOfIntegrations[session.id])
                        mapOfIntegrations.remove(session.id)
                    })
            session.send(publisher)
        }

        mapping.urlMap = mapOf<String, WebSocketHandler>("/websocket/updates" to webSocketHandler)
        mapping.order = 10
        return mapping
    }

    @Bean
    fun webSocketHandlerAdapter() = WebSocketHandlerAdapter()
}

fun main(args: Array<String>) {
    SpringApplication.run(ServiceApplication::class.java, *args)
}
