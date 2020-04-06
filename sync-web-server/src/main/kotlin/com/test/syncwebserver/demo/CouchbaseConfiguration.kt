package com.test.syncwebserver.demo

import com.couchbase.client.core.cnc.tracing.NoopRequestTracer
import com.couchbase.client.core.env.CompressionConfig
import com.couchbase.client.core.env.TimeoutConfig
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.ClusterOptions
import com.couchbase.client.java.Collection
import com.couchbase.client.java.codec.JacksonJsonSerializer
import com.couchbase.client.java.env.ClusterEnvironment
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class CouchbaseConfiguration(private val objectMapper: ObjectMapper) {
    @Bean
    fun collection(): Collection {
        val env = ClusterEnvironment
            .builder()
            .compressionConfig(CompressionConfig.builder().enable(true))
            .timeoutConfig(TimeoutConfig.builder().kvTimeout(Duration.ofMillis(500)))
            .requestTracer(NoopRequestTracer())
            .jsonSerializer(JacksonJsonSerializer.create(objectMapper))
            .build()
        val clusterOptions = ClusterOptions.clusterOptions("test", "123qwe")
            .environment(env)

        val cluster = Cluster.connect("localhost", clusterOptions)
        cluster.waitUntilReady(Duration.ofMinutes(1))
        val bucket = cluster.bucket("User").also {
            it.waitUntilReady(Duration.ofSeconds(30))
        }
        return bucket.defaultCollection()
    }
}