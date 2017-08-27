package com.example.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import java.time.Duration
import java.util.*


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
    fun webSocketHandlerMapping(): HandlerMapping {

        val mapping = SimpleUrlHandlerMapping()

        val webSocketHandler = WebSocketHandler { session ->
            val x = Flux.generate({ x: SynchronousSink<String> -> x.next(om.writeValueAsString(CustomerUpdateEvent(Date()))) })
                    .delayElements(Duration.ofSeconds(20L))
                    .map { session.textMessage(it) }
            session.send(x)
        }

        mapping.urlMap = mapOf<String, WebSocketHandler>("/websocket/updates" to webSocketHandler)
        mapping.order = 10
        return mapping
    }

    @Bean
    fun webSocketHandlerAdapter() = WebSocketHandlerAdapter()

}

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

data class CustomerUpdateEvent(var date: Date)

interface CustomerRepository : ReactiveMongoRepository<Customer, String>

@Document
class Customer(@Id var id: String? = null, var name: String? = null) {

    constructor() : this(null, null)

    override fun toString(): String = """ Customer( id="${this.id}" name="${this.name}" ) """.trim()
}