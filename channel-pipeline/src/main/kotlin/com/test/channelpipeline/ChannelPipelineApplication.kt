package com.test.channelpipeline

import com.couchbase.client.java.ReactiveCollection
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

@SpringBootApplication
class ChannelPipelineApplication

fun main(args: Array<String>) {
    runApplication<ChannelPipelineApplication>(*args)
}

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@Component
class RedditPostProcessPipeline(private val redditHttpService: RedditHttpService, private val reactiveCollection: ReactiveCollection) : CoroutineScope {
    private val producerContext = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    private val writerContext = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
    private val enricherContext = Executors.newFixedThreadPool(3).asCoroutineDispatcher()

    override val coroutineContext: CoroutineContext get() = Dispatchers.IO

    @EventListener(ApplicationStartedEvent::class)
    fun processRedditPosts() {
        val raw = producerActor()
        val enriched = enricherActor(raw)

        val completed = Channel<Finished>(5)

        repeat(5) { // launch 5 writer in parallel
            writerActor(enriched, completed)
        }

        do {
            println("Writer process ongoing...")
        } while (!completed.isEmpty)

        println("All messages processed")
        coroutineContext.cancelChildren()
    }

    fun CoroutineScope.producerActor() = produce<Envelope<RedditPost>>(
        producerContext,
        capacity = 10,
        onCompletion = { producerContext.close() }
    ) {
        val redditPosts = redditHttpService.getTopPosts()

        redditPosts.forEach {
            channel.send(Envelope(it))
        }
        channel.close()
    }

    fun CoroutineScope.enricherActor(inbox: ReceiveChannel<Envelope<RedditPost>>): ReceiveChannel<Envelope<RedditPost>> =
        produce(
            enricherContext,
            capacity = 10,
            onCompletion = { enricherContext.close() }
        ) {
            for (msg in inbox) {
                with(msg) {
                    val imageBuffer = redditHttpService.getRawThumbnail(payload.thumbnailUrl)
                    val rawImage = String(Base64.getEncoder().encode(imageBuffer.asByteBuffer()).array())

                    channel.send(Envelope(RedditPost(payload.title, rawImage)))
                }
            }
        }

    fun CoroutineScope.writerActor(inbox: ReceiveChannel<Envelope<RedditPost>>, writingFinished: Channel<Finished>): ReceiveChannel<Envelope<RedditPostRawData>> =
        produce(
            writerContext,
            capacity = 100,
            onCompletion = { writerContext.close() }
        )
        {
            try {
                for (msg in inbox) {
                    reactiveCollection.insert(UUID.randomUUID().toString(), msg.payload).awaitFirstOrNull()
                    writingFinished.send(Finished)
                }
            } catch (e: Exception) {
                println(e)
            }
        }
}

@Service
class RedditHttpService(webClientBuilder: WebClient.Builder) {
    var webClient: WebClient = webClientBuilder.build()

    suspend fun getTopPosts(): List<RedditPost> {
        val data = webClient.get().uri("https://www.reddit.com/r/all/hot.json").retrieve().bodyToMono(String::class.java).awaitFirst()
        return JSONObject(data).getTitleThumbnailPair().filter { it.thumbnailUrl.isValidURL() }
    }

    suspend fun getRawThumbnail(url: String): DataBuffer {
        return webClient.get().uri(url).retrieve().bodyToFlux(DataBuffer::class.java).awaitFirst()
    }

    fun JSONObject.getTitleThumbnailPair(): List<RedditPost> {
        return ((this.get("data") as JSONObject).get("children") as JSONArray).map {
            RedditPost(
                ((it as JSONObject).get("data") as JSONObject).get("title") as String,
                (it.get("data") as JSONObject).get("thumbnail") as String
            )
        }
    }

    fun String.isValidURL(): Boolean {
        return try {
            val url = URL(this)
            true
        } catch (e: MalformedURLException) {
            false
        }
    }
}

//region Models

data class Envelope<T>(val payload: T)

data class RedditPost(val title: String, val thumbnailUrl: String)

data class RedditPostRawData(val title: String, val thumbnailRawData: String)

object Finished

//endregion