package exception

import com.google.gson.Gson

class Message<T>(val shortcut: Shortcut, private val msg: String, val data: T) {
    fun json(): String {
        return gson.toJson(Message(shortcut, msg, data), Message::class.java)
    }

    companion object {
        private val gson = Gson()
    }
}