package com.test.coroutinewebserver

import com.couchbase.client.java.ReactiveCollection
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@SpringBootApplication
class CoroutineWebServerApplication

fun main(args: Array<String>) {
    runApplication<CoroutineWebServerApplication>(*args)
}

@RestController
class UserController(private val userService: UserService) {
    @GetMapping("users/{id}")
    suspend fun getUser(@PathVariable id: String): User {
        return userService.getUser(id)
    }
}

@Service
class UserService(private val userRepository: UserRepository, private val addressRepository: AddressRepository) {
    suspend fun getUser(id: String): User {
        return userRepository.getUser(id)
    }

    suspend fun getUserWithAddress(id: String): UserWithAddressDto {
        val user = userRepository.getUser(id)

        if (user.hasAddress) {
            val address = addressRepository.getAddressOfUser(id)
            return UserWithAddressDto(user.firstName, address.address)
        }

        return UserWithAddressDto(user.firstName, "No Address")
    }
}

@Component
class UserRepository(private val reactiveCollection: ReactiveCollection) {
    suspend fun getUser(id: String): User {
        return reactiveCollection.get(id).map { it.contentAs(User::class.java) }.awaitFirstOrNull() ?: throw Exception("user.not.found")
    }
}

@Component
class AddressRepository(private val reactiveCollection: ReactiveCollection) {
    suspend fun getAddressOfUser(userId: String): Address {
        return reactiveCollection.get(userId).awaitFirst().contentAs(Address::class.java)
    }
}

//region Models
data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val hasAddress: Boolean
)

data class Address(
    val userId: String,
    val address: String
)

data class UserWithAddressDto(
    val userName: String,
    val address: String
)
//endregion