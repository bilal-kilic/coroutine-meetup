package com.test.coroutinewebserver

import com.couchbase.client.core.cnc.tracing.NoopRequestTracer
import com.couchbase.client.core.env.CompressionConfig
import com.couchbase.client.core.env.TimeoutConfig
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.ClusterOptions
import com.couchbase.client.java.Collection
import com.couchbase.client.java.ReactiveCollection
import com.couchbase.client.java.codec.JacksonJsonSerializer
import com.couchbase.client.java.env.ClusterEnvironment
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.scheduler.Schedulers
import java.time.Duration

@Configuration
class CouchbaseConfiguration(private val objectMapper: ObjectMapper) {
    @Bean
    fun collection(): ReactiveCollection {
        val env = ClusterEnvironment
            .builder()
            .compressionConfig(CompressionConfig.builder().enable(true))
            .timeoutConfig(TimeoutConfig.builder().kvTimeout(Duration.ofMillis(500)))
            .timeoutConfig(TimeoutConfig.builder().queryTimeout(Duration.ofMillis(500)))
            .requestTracer(NoopRequestTracer())
            .jsonSerializer(JacksonJsonSerializer.create(objectMapper))
            .scheduler(Schedulers.elastic())
            .build()
        val clusterOptions = ClusterOptions.clusterOptions("test", "123qwe")
            .environment(env)

        val cluster = Cluster.connect("localhost", clusterOptions)
        cluster.waitUntilReady(Duration.ofMinutes(1))
        val bucket = cluster.bucket("User").also {
            it.waitUntilReady(Duration.ofSeconds(30))
        }
        return bucket.defaultCollection().reactive()
    }
}