package com.redbubble.util.json

import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newFixedThreadPool

import com.redbubble.util.http.{JsonApiClient, RelativePath}
import com.redbubble.util.log.Logger
import com.redbubble.util.metrics.StatsReceiver
import com.redbubble.util.spec.SpecHelper
import com.twitter.util.FuturePool
import io.circe.Decoder
import org.specs2.mutable.Specification

final class JsonApiClientSpec extends Specification with SpecHelper {
  "Endpoint call" >> {
    "Make a call to jsonplaceholder" >> {
      "We get a result back" >> {
        val postDecoder: Decoder[Any] = Decoder.instance { c =>
          c.downField("title").as[String]
        }

        val executorService: ExecutorService = newFixedThreadPool(1)
        val futurePool: FuturePool = FuturePool.interruptible(executorService)

        val baseMetrics: StatsReceiver = StatsReceiver.stats
        val logger = new Logger(s"test logger")(futurePool)
        val userAgent = s"RB-SCALA-UTILS"
        val baseUrl: URL = new URL(s"https://jsonplaceholder.typicode.com/")
        val client = featherbed.Client(baseUrl)
        val apiClient = JsonApiClient.client(client, userAgent, baseUrl.getHost)(baseMetrics, logger)
        val result = apiClient.get(RelativePath("posts/1"))(postDecoder)
        result must be(result)
      }
    }
  }
}
