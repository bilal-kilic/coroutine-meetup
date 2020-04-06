package com.test.syncwebserver.demo

import com.couchbase.client.java.Collection
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class SyncWebServerApplication

fun main(args: Array<String>) {
    runApplication<SyncWebServerApplication>(*args)
}

@RestController
class UserController(private val userService: UserService) {
    @GetMapping("users/{id}")
    fun getUser(@PathVariable id: String): User {
        return userService.getUser(id)
    }
}

@Service 
class UserService(private val userRepository: UserRepository) {
    fun getUser(id: String): User {
        return userRepository.getUser(id)
    }
}

@Service
class UserRepository(private val collection: Collection) {
    fun getUser(id: String): User {
        return collection.get(id).contentAs(User::class.java)
    }
}

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String
)