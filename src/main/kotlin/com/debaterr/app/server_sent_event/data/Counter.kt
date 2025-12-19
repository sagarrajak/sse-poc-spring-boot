package com.debaterr.app.server_sent_event.data

data class Counter(var id: String, var likes: Int = 0, var dislikes: Int = 0);
data class WatchRequest(
    var ids: MutableSet<String>
)