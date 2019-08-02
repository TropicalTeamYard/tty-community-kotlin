package util

import model.UserInfoType

import java.util.Date

object Log {
    fun register(id: String, date: Date, ip: String, nickname: String) {
        val log = "user::register::nickname=$nickname::time=${StringUtil.getTime(date)}::ip=$ip\n"
        logUser(id, log)
    }

    fun login(id: String, date: Date, ip: String, loginType: LoginType, loginPlatform: LoginPlatform) {
        val log = "user::login::success::time=${StringUtil.getTime(date)}::login_type=${loginType.name}::platform=${loginPlatform.name}::ip=$ip\n"
        logUser(id, log)
    }

    fun loginFailed(id: String, date: Date, ip: String, loginType: LoginType, loginPlatform: LoginPlatform) {
        val log = "user::login::failed::time=${StringUtil.getTime(date)}::login_type=${loginType.name}::platform=${loginPlatform.name}::ip=$ip\n"
        logUser(id, log)
    }

    fun autoLogin(id: String, date: Date, ip: String, loginPlatform: LoginPlatform) {
        val log = "user::auto_login::success::time=${StringUtil.getTime(date)}::platform=${loginPlatform.name}::ip=$ip\n"
        logUser(id, log)
    }

    fun autoLoginFailed(id: String, date: Date, ip: String, loginPlatform: LoginPlatform) {
        val log = "user::auto_login::invalid_token::time=${StringUtil.getTime(date)}::platform=${loginPlatform.name}::ip=$ip\n"
        logUser(id, log)
    }

    fun changeUserInfo(id: String, date: Date, ip: String, status: Boolean, before: String? = null, after: String? = null, target: UserInfoType? = null)  {
        val log = "user::change_info::${
            when(status){
                true -> "success::time=${StringUtil.getTime(date)}::ip=$ip::target=${target?:UserInfoType.Default.name}::before=$before::after=$after"
                false -> "failed::time=${StringUtil.getTime(date)}::ip=$ip::target=${target?:UserInfoType.Default.name}"
            }
        }\n"
        logUser(id, log)
    }

    fun changeUserDetailInfo(id: String, date: Date, ip: String, status: Boolean, before: String? = null, after: String? = null, target: String? = null)  {
        val log = "user::change_info::ip=$ip::${
        when(status){
            true -> "success::time=${StringUtil.getTime(date)}::target=${target?:"default"}::before=$before::after=$after"
            false -> "failed::time=${StringUtil.getTime(date)}::target=${target?:"default"}"
        }
        }\n"
        logUser(id, log)
    }

    fun changePassword(id: String, date: Date, ip: String, status: Boolean) {
        val log = "user::change_password::${
            when(status){
                true -> "success"
                false -> "failed"
            }
        }::ip=$ip::date=${StringUtil.getTime(date)}\n"
        logUser(id, log)
    }



    fun createBlog(id: String, date: Date, ip: String, status: Boolean, blogId: String? = null){
        val user = "blog::create::${
            when(status){
                true -> "success::ip=$ip::date=${StringUtil.getTime(date)}::blogId=$blogId"
                false -> "failed::ip=$ip::date=${StringUtil.getTime(date)}"
            }
        }\n"

        logUser(id, user)

        if (status && blogId != null) {
            val blog = "blog::create::success::date=${StringUtil.getTime(date)}::ip=$ip::author=$id\n"
            logBlog(blogId, blog)
        }


    }




    private fun logBlog(blogId:String, log: String){
        val conn = MySQLConn.connection
        val ps = conn.prepareStatement("update blog set log = concat(?, log) where blog_id = ?")
        ps.setString(1, log)
        ps.setString(2, blogId)
        ps.executeUpdate()
        ps.close()
    }

    private fun logUser(id: String, log: String) {
        val conn = MySQLConn.connection
        val ps = conn.prepareStatement("update user_detail set log = concat(?, log) where id = ?")
        ps.setString(1, log)
        ps.setString(2, id)
        ps.executeUpdate()
        ps.close()
    }
}

enum class Shortcut{
    AE, FE, UR, OK,
    UNE, UPE,
    TE,
    BNE,
    OTHER
}

internal object Token{
    //id platform secret time
    fun getToken(id: String, platform: LoginPlatform, secret: String, time: Date, status: Boolean): String {
        return "$id::${platform.name}::$secret::${StringUtil.getTime(time)}::$status"
    }
}