package com.test.reactorwebserver.demo

import com.couchbase.client.java.ReactiveCollection
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@RestController
class UserController(private val userService: UserService) {
    @GetMapping("users/{id}")
    fun getUser(@PathVariable id: String): Mono<User> {
        return userService.getUser(id)
    }

    @GetMapping("users/{id}/with-address")
    fun getUserWithAddress(@PathVariable id: String) {
        userService.getUserWithAddress(id)
    }
}

@Service
class UserService(private val userRepository: UserRepository, private val addressRepository: AddressRepository) {
    fun getUser(id: String): Mono<User> {
        return userRepository.getUser(id)
    }

    fun getUserWithAddress(id: String): Mono<UserWithAddressDto> {
        val user = userRepository.getUser(id)

        return user.flatMap { usr ->
            if (usr.hasAddress) {
                val address = addressRepository.getAddressOfUser(usr.id)
                return@flatMap address.map { adr -> UserWithAddressDto(usr.firstName, adr.address) }
            } else {
                return@flatMap Mono.just(UserWithAddressDto(usr.firstName, "No address found."))
            }
        }
    }
}

@Service
class UserRepository(private val reactiveCollection: ReactiveCollection) {
    fun getUser(id: String): Mono<User> {
        return reactiveCollection.get(id).map { it.contentAs(User::class.java) }
    }
}

@Component
class AddressRepository {
    fun getAddressOfUser(userId: String): Mono<Address> {
        return Mono.justOrEmpty(Address(userId, "Istanbul/Turkey"))
    }
}

//region Models
data class Address(
    val userId: String,
    val address: String
)

data class UserWithAddressDto(
    val userName: String,
    val address: String
)

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val hasAddress: Boolean
)
//endregion