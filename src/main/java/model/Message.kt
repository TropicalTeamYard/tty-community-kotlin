package model

import com.google.gson.Gson
import util.enums.Shortcut

class Message<T>(val shortcut: Shortcut, val msg: String, val data: T) {
    fun json(): String {
        return gson.toJson(Message(Shortcut.OK, msg, data), Message::class.java)
    }

    companion object {
        private val gson = Gson()
    }
}