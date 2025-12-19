package com.debaterr.app.server_sent_event.services

import com.debaterr.app.server_sent_event.SseService
import com.debaterr.app.server_sent_event.data.Counter
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class CounterService(
    private val sseService: ServerSentEventServices
) {
    //id for the user
    private var counter = ConcurrentHashMap<String, Counter>();

    fun getCounter(id: String): Counter {
        return counter.getOrPut(id) {Counter(id)}
    }

    fun updateLikeCounter(id: String, likes: Int, dislikes: Int) {
        val counter = counter.getOrPut(id) { Counter(id) }
        counter.likes = likes
        counter.dislikes = dislikes
        sseService.broadCast(counter);
    }
}