package util

import model.UserInfoType

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
    fun autoLogin(date: Date, ip: String, loginPlatform: LoginPlatform): String{
        return "user::auto_login::success::time=${StringUtil.getTime(date)}::platform=${loginPlatform.name}::ip=$ip\n"
    }
    fun autoLoginFailed(date: Date, ip: String, loginPlatform: LoginPlatform): String{
        return "user::auto_login::invalid_token::time=${StringUtil.getTime(date)}::platform=${loginPlatform.name}::ip=$ip\n"
    }
    fun changeUserInfo(date: Date, ip: String, status: Boolean, before: String? = null, after: String? = null, target: UserInfoType? = null): String {
        return "user::change_info::ip=$ip::${
            when(status){
                true -> "success::time=${StringUtil.getTime(date)}::target=${target?:UserInfoType.Default.name}::before=$before::after=$after"
                false -> "failed"
            }
        }\n"
    }
    fun changePassword(date: Date, ip: String, status: Boolean): String{
        return "user::change_password::${
            when(status){
                true -> "success"
                false -> "failed"
            }
        }::ip=$ip::date=${StringUtil.getTime(date)}"
    }
}

enum class Shortcut{
    AE, FE, UR, OK,
    UNE, UPE,
    TE,
    OTHER
}

internal object Token{
    //id
    //platform
    //secret
    //time
    fun getToken(id: String, platform: LoginPlatform, secret: String, time: Date, status: Boolean): String {
        return "$id::${platform.name}::$secret::${StringUtil.getTime(time)}::$status"
    }
}