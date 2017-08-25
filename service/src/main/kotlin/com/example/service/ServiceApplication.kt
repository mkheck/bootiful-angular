package com.example.service

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@SpringBootApplication
class ServiceApplication(val customerRepository: CustomerRepository) : ApplicationRunner {

    override fun run(p0: ApplicationArguments?) {
        val flatMap = Flux.just("Jane", "Bob", "Michelle", "George")
                .map { Customer(name = it) }
        customerRepository.saveAll(flatMap).subscribe()
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(ServiceApplication::class.java, *args)
}

@RestController
@CrossOrigin (origins = arrayOf("*"))
class ReactiveCustomerRestController(
        val customerRepository: CustomerRepository) {

    @GetMapping("/customers")
    fun customers(): Flux<Customer> = customerRepository.findAll()
}

interface CustomerRepository : ReactiveMongoRepository<Customer, String>

@Document
class Customer(@Id var id: String? = null, var name: String? = null) {
    constructor() : this(null, null)
}