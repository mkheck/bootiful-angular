package com.example.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.integration.channel.PublishSubscribeChannel
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.file.dsl.Files
import org.springframework.messaging.MessageHandler
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.io.File
import java.util.*
import java.util.function.Consumer


@SpringBootApplication
class ServiceApplication(val customerRepository: CustomerRepository) {

    @Bean
    fun init() = ApplicationRunner {
        val flatMap = Flux
                .just("Tammie", "Richard", "Michelle", "Mario")
                .map { Customer(name = it) }

        customerRepository
                .deleteAll()
                .thenMany(customerRepository.saveAll(flatMap))
                .subscribe({ println(it) })
    }
}

@Configuration
class WebSocketConfiguration {


    private val om = ObjectMapper()

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
        val webSocketHandler = WebSocketHandler { session ->
            val publisher = Flux.create(Consumer<FluxSink<WebSocketMessage>> { sink ->

                val incomingFiles = MessageHandler {
                    val payload = it.payload as File
                    val updatedPayload = mapOf<String, String>("sessionId" to session.id, "path" to payload.absolutePath)
                    val jsonPayload = om.writeValueAsString(updatedPayload)
                    sink.next(session.textMessage(jsonPayload))
                }

                session.receive().doFinally({
                    channel.unsubscribe(incomingFiles)
                    println( "ending the Spring Integration subscription for session ${session.id}")
                })

                channel.subscribe(incomingFiles)

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

/*
class MessageProducingWebSocketHandler : WebSocketHandler {

    private val om = ObjectMapper()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val x = Flux.generate({ x: SynchronousSink<String> -> x.next(om.writeValueAsString(CustomerUpdateEvent(Date()))) })
                .map { session.textMessage(it) }
        return session.send(x)
    }
}

class EchoWebSocketHandler : WebSocketHandler {

    private val om = ObjectMapper()

    override fun handle(session: WebSocketSession): Mono<Void> {

        val incoming = session
                .receive()
                .map { it.payloadAsText }
                .map { session.textMessage(it) }

        return session.send(incoming)
    }
}
*/


/*

class CustomerWebSocketHandler : WebSocketHandler {

    private val om = ObjectMapper()

    override fun handle(session: WebSocketSession): Mono<Void> {

        val interval = Flux.interval(Duration.ofSeconds(1))
        val msgs = Flux.generate<CustomerUpdateEvent> { it.next(CustomerUpdateEvent(Date())) }
        val txtMsgs = Flux
                .zip(interval, msgs)
                .map { it.t2 }
                .map {
                    println("${it}")
                    session.textMessage(om.writeValueAsString(it))
                }
        session.send(txtMsgs)
        return Mono.empty()
    }
}
*/

fun main(args: Array<String>) {
    SpringApplication.run(ServiceApplication::class.java, *args)
}

@RestController
@CrossOrigin(origins = arrayOf("*"))
class ReactiveCustomerRestController(val customerRepository: CustomerRepository) {

    @GetMapping("/customers")
    fun customers(): Flux<Customer> = customerRepository.findAll()
}

// todo
data class CustomerUpdateEvent(var date: Date)

interface CustomerRepository : ReactiveMongoRepository<Customer, String>

@Document
class Customer(@Id var id: String? = null, var name: String? = null) {

    constructor() : this(null, null)

    override fun toString(): String = """ Customer( id="${this.id}" name="${this.name}" ) """.trim()
}