package model

import com.alibaba.fastjson.JSONObject
import util.LoginPlatform
import util.LoginType
import util.MySQLConn
import util.StringUtil
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*
import kotlin.collections.HashMap

class Login(
    private var ip: String?,
    private var platform: LoginPlatform?
) {
    var loginType: LoginType? = null
    var id: String? = null
    var nickname: String? = null
    var password: String? = null
    var APIKey: String? = null
    private val loginTime = Date()

    fun submit(): String{
        val conn = MySQLConn.mySQLConnection
        if(
            ip.isNullOrEmpty()||ip == "0.0.0.0"
            ||platform == null
            ||loginType == null
            ||(loginType == LoginType.ID && (id.isNullOrEmpty() || password.isNullOrEmpty()))
            ||(loginType == LoginType.NICKNAME && (nickname.isNullOrEmpty()) || password.isNullOrEmpty())
            ||(loginType == LoginType.THIRD_PARTY && (id.isNullOrEmpty() || APIKey.isNullOrEmpty()))
        ){
            return json(Shortcut.AE, "arguments mismatch.")
        } else {
            try {
                var ps: PreparedStatement? = null
                when(loginType){
                    LoginType.ID -> {
                        ps = conn.prepareStatement("select * from user where id = ? and password = ? limit 1")
                        ps.setString(1, id)
                        ps.setString(2, password)
                    }
                    LoginType.NICKNAME -> {
                        ps = conn.prepareStatement("select * from user where nickname = ? and password = ? limit 1")
                        ps.setString(1, nickname)
                        ps.setString(2, password)
                    }
                    LoginType.THIRD_PARTY -> {
                        return json(Shortcut.OTHER, "method not supported yet.")
                    }
                }

                val rs = ps!!.executeQuery()

                if(rs.next()){
                    val data = HashMap<String, String>()
                    id = rs.getString("id")?:"null"
                    nickname = rs.getString("nickname")
                    data["id"] = id!!
                    data["nickname"] = nickname!!
                    data["email"] = rs.getString("email")
                    rs.close()
                    ps.close()
                    val token = Token.getToken(id!!, platform!!, "123456", loginTime, true)
                    ps = conn.prepareStatement("update user set last_login_time = ?, last_login_ip = ?, token = ?, log = concat(?, log)  where id = ?")
                    ps.setString(1, StringUtil.getTime(loginTime))
                    ps.setString(2, ip)
                    ps.setString(3, token)
                    ps.setString(4, Log.login(loginTime, ip!!, loginType!!, platform!!))
                    ps.setString(5, id)
                    ps.executeUpdate()
                    data["token"] = StringUtil.getMd5(token)?:"00000000000000000000000000000000"
                    ps.close()
                    return json(Shortcut.OK, "Ok, let's fun", data)
                } else {
                    rs.close()
                    ps.close()
                    when(loginType) {
                        LoginType.ID -> {
                            ps = conn.prepareStatement("update user set log = concat(?, log) where id = ?")
                            ps.setString(1, Log.loginFailed(loginTime, ip!!, loginType!!, platform!!))
                            ps.setString(2, id)
                            ps.executeUpdate()
                            ps.close()
                        }
                        LoginType.NICKNAME -> {
                            ps = conn.prepareStatement("update user set log = concat(?, log) where nickname = ?")
                            ps.setString(1, Log.loginFailed(loginTime, ip!!, loginType!!, platform!!))
                            ps.setString(2, nickname)
                            ps.executeUpdate()
                            ps.close()
                        }
                        LoginType.THIRD_PARTY -> {

                        }
                    }

                    return json(Shortcut.UPE, "wrong password.")
                }

            } catch (e: SQLException){
                e.printStackTrace()
                return json(Shortcut.OTHER, "SQL ERROR")
            }

        }

    }

    companion object {
        private fun json(shortcut: Shortcut, msg: String, data: HashMap<String, String>?=null): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            if(data!=null){map["data"] = JSONObject(data as Map<String, Any>?)}
            return map.toJSONString()
        }
    }
}


class AutoLogin(val ip: String?, var id: String?, var token: String?, var platform: LoginPlatform) {
    private val loginTime = Date()
    private val conn = MySQLConn.mySQLConnection
    fun submit(): String{
        if(ip.isNullOrEmpty() || ip == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()){
            return json(Shortcut.AE, "arguments mismatch.")
        } else {
            try {
                var ps = conn.prepareStatement("select * from user where id = ?")
                ps.setString(1, id)
                val rs = ps.executeQuery()
                if (rs.next()){
                    val token = rs.getString("token")
                    return if(StringUtil.getMd5(token) == this.token){
                        val data= HashMap<String, String>()
                        data["id"] = rs.getString("id")
                        data["email"] = rs.getString("email")
                        rs.close()
                        ps.close()
                        val log = Log.autoLogin(loginTime, ip, platform)
                        ps = conn.prepareStatement("update user set log = concat(?, log), last_login_ip = ?, last_login_time = ? where id = ?")
                        ps.setString(1, log)
                        ps.setString(2, ip)
                        ps.setString(3, StringUtil.getTime(loginTime))
                        ps.setString(4, id)
                        ps.executeUpdate()
                        ps.close()
                        json(Shortcut.OK, "OK, let's fun", data)
                    } else {
                        rs.close()
                        ps.close()
                        val log = Log.autoLoginFailed(loginTime, ip, platform)
                        ps = conn.prepareStatement("update user set log = concat(?, log) where id = ?")
                        ps.setString(1, log)
                        ps.setString(2, id)
                        ps.executeUpdate()
                        ps.close()
                        json(Shortcut.TE, "invalid token")
                    }
                } else {
                    ps.close()
                    return json(Shortcut.UPE, "user $id not exist.")
                }
            } catch (e: SQLException){
                e.printStackTrace()
                return json(Shortcut.OTHER, "SQL ERROR")
            }


        }
    }

    companion object {
        private fun json(shortcut: Shortcut, msg: String, data: HashMap<String, String>?=null): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            if(data!=null){map["data"] = JSONObject(data as Map<String, Any>?)}
            return map.toJSONString()
        }
    }
}