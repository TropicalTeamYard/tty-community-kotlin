package model

import com.alibaba.fastjson.JSONObject
import util.StringUtil
import java.awt.MenuShortcut

import java.util.Date

internal object Log {
    fun register(date: Date, ip: String, nickname: String): String {
        // user::register::nickname=feifei::time=2019/7/14-19:04:55::ip=192.168.123.186
        return "user::register::nickname=$nickname::time=${StringUtil.getTime(date)}::ip=$ip\n"
    }
}

enum class Shortcut{
    AE, FE, UR, OK,
    UNE, UPE,
    OTHER
}

internal object Token{
    //id
    //client
    //secret
    //time
    fun getToken(id: String, client: String, secret: String, time: Date): String {
        return "$id::$client::$secret::${StringUtil.getTime(time)}"
    }
}