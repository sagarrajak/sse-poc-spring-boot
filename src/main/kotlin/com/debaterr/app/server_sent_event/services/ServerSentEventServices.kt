package com.debaterr.app.server_sent_event.services

import com.debaterr.app.server_sent_event.data.Counter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import tools.jackson.databind.ObjectMapper
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class ServerSentEventServices {
    // key -> seassonId, value -> SseEmiter
    val activaeSeassnMap = ConcurrentHashMap<String, MutableSet<String>>();
    val emitterMap = ConcurrentHashMap<String, SseEmitter>();
    val objectMapper = ObjectMapper();

    companion object {
        val log = LoggerFactory.getLogger(ServerSentEventServices::class.java)
    }
    //Register the season
    fun getSeassonId(ids: MutableSet<String>): String {
        val seasonId = UUID.randomUUID().toString()
        activaeSeassnMap[seasonId]?.clear()
        activaeSeassnMap[seasonId] = ids;
        return seasonId;
    }

    fun getActiveSeason(seasonId: String): MutableSet<String>? {
        return activaeSeassnMap[seasonId];
    }

    fun getEmitter(season: String): SseEmitter? {
        emitterMap[season] = SseEmitter(Long.MAX_VALUE);
        emitterMap[season]?.onError { cleanUp(season) }
        emitterMap[season]?.onTimeout() { cleanUp(season) }
        emitterMap[season]?.onError { cleanUp(season) }
        return emitterMap[season];
    }

    fun cleanUp(sessionId: String) {
        emitterMap.remove(sessionId)
        activaeSeassnMap.remove(sessionId);
    }

    fun broadCast(counter: Counter) {
        // find all the season with id
        val activeSeassons = activaeSeassnMap.filter { entry ->
            entry.value.contains(counter.id)
        }.keys

        activeSeassons.forEach { season ->
            val emitter = emitterMap[season]
            if (emitter != null) {
                try {
                    emitter.send(
                        SseEmitter.event()
                            .name("counter-update")
                            .data(counter)
                            .id(counter.id)
                            .reconnectTime(3000)
                            .build()
                    )
                } catch (e: IOException) {
                    log.error("Unable to send event ${e.message}")
                    log.error(e.toString());
                }
            }
        }
    }
}