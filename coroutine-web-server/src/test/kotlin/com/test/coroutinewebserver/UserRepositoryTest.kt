package com.test.coroutinewebserver

import com.couchbase.client.java.ReactiveCollection
import com.couchbase.client.java.kv.GetResult
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Mono
import java.util.*

@ExtendWith(MockKExtension::class)
internal class UserRepositoryTest {
    @InjectMockKs
    private lateinit var userRepository: UserRepository

    @RelaxedMockK
    private lateinit var reactiveCollection: ReactiveCollection

    @Test
    fun getUser() = runBlockingTest {
        //given
        val id = UUID.randomUUID().toString()
        val user = User(id, "Bilal", "Kilic", "bilal.kilic@trendyol.com", true)
        val getResult = mockk<GetResult>()
        
        coEvery { reactiveCollection.get(id) } returns Mono.just(getResult)
        coEvery { getResult.contentAs(User::class.java) } returns user

        //when
        val result = userRepository.getUser(id)

        //then    
        assertThat(result).isEqualTo(user)
    }
}