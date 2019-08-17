package model

import com.google.gson.Gson
import enums.Shortcut

class Message<T>(val shortcut: Shortcut, val msg: String, val data: T? = null) {
    fun json(): String {
        return gson.toJson(Message(shortcut, msg, data), Message::class.java)
    }

    companion object {
        private val gson = Gson()
    }
}