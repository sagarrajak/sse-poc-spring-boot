package com.debaterr.app.server_sent_event.controller

import com.debaterr.app.server_sent_event.data.Counter
import com.debaterr.app.server_sent_event.data.WatchRequest
import com.debaterr.app.server_sent_event.services.CounterService
import com.debaterr.app.server_sent_event.services.ServerSentEventServices
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.Executors

@RestController
@RequestMapping("/sse-event")
class StreamController(
    private val sseService: ServerSentEventServices,
    private val counterService: CounterService
) {

    companion object {
        private val log = LoggerFactory.getLogger(StreamController::class.java);
    }

    private val excecuter = Executors.newCachedThreadPool();

    @GetMapping("/stream-sse")
    fun streamEvent(): SseEmitter {
        val sseEmitter = SseEmitter(Long.MAX_VALUE)
        excecuter.execute {
            for (i in 0 until 1000) {
                Thread.sleep(1000)
                sseEmitter.send("SSE MVC - " + System.currentTimeMillis());
            }
        }
        return sseEmitter
    }


    @PostMapping("/get-season")
    fun getSeason(
        @RequestBody body: WatchRequest
    ): Map<String, String> {
        log.info("got request with body {}", body);
        val seasonId = sseService.getSeassonId(body.ids)
        return mapOf("season" to seasonId)
    }


    @GetMapping("/stream/{seasonId}",   produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamEvent(@PathVariable seasonId: String): SseEmitter? {
        return sseService.getEmitter(seasonId).apply {
            val activeSeason = sseService.getActiveSeason(seasonId)
            activeSeason?.forEach { id ->
                val counter = counterService.getCounter(id)
                try {
                    this?.send(
                        SseEmitter
                            .event()
                            .name("counter-event")
                            .data(counter)
                            .id(counter.id)
                            .build()
                    )
                } catch (e: IOException) {
                    ServerSentEventServices.Companion.log.error("Unable to send event ${e.message}")
                    ServerSentEventServices.Companion.log.error(e.toString());
                }
            }
        }
    }

    @PostMapping("/counters")
    fun updateCounter(@RequestBody update: Counter) {
        counterService.updateLikeCounter(update.id, update.likes, update.dislikes)
    }
}