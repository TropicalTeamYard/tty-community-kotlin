package model

import util.LoginPlatform
import util.LoginType
import util.StringUtil

import java.util.Date

internal object Log {
    fun register(date: Date, ip: String, nickname: String): String {
        // user::register::nickname=feifei::time=2019/7/14-19:04:55::ip=192.168.123.186
        return "user::register::nickname=$nickname::time=${StringUtil.getTime(date)}::ip=$ip\n"
    }
    fun login(date: Date, ip: String, loginType: LoginType, loginPlatform: LoginPlatform): String{
        return "user::login::success::time=${StringUtil.getTime(date)}::login_type=${loginType.name}::platform=${loginPlatform.name}::ip=$ip\n"
    }
    fun loginFailed(date: Date, ip: String, loginType: LoginType, loginPlatform: LoginPlatform): String{
        return "user::login::failed::time=${StringUtil.getTime(date)}::login_type=${loginType.name}::platform=${loginPlatform.name}::ip=$ip\n"
    }
}

enum class Shortcut{
    AE, FE, UR, OK,
    UNE, UPE,
    OTHER
}

internal object Token{
    //id
    //platform
    //secret
    //time
    fun getToken(id: String, platform: LoginPlatform, secret: String, time: Date): String {
        return "$id::${platform.name}::$secret::${StringUtil.getTime(time)}"
    }
}