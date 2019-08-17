package model

import enums.Shortcut
import util.CONF

class Message<T>(val shortcut: Shortcut, val msg: String, val data: T? = null) {
    fun json(): String {
        return gson.toJson(Message(shortcut, msg, data), Message::class.java)
    }

    companion object {
        private val gson = CONF.gson
    }
}