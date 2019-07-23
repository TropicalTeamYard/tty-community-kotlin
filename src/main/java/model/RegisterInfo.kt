package model

import com.alibaba.fastjson.JSONObject
import util.LoginPlatform
import util.MySQLConn
import util.StringUtil
import java.sql.SQLException
import java.util.*


class RegisterInfo(
    private val nickname: String?,
    private val registerIP: String?,
    private val email: String?,
    private val password: String?
) {
    fun submit(): String {
        //TODO CHECK WHETHER USER INFO IS VALID
        val conn = MySQLConn.mySQLConnection
        if(nickname == null|| registerIP == null || email == null || password ==null || nickname.isEmpty()||registerIP.isEmpty()||registerIP=="0.0.0.0"||email.isEmpty()||password.isEmpty()){
            return Companion.json(Shortcut.AE, "arguments mismatch.")
        }
        try {
            var ps = conn.prepareStatement("select * from user where nickname = ?")
            ps.setString(1, nickname)
            val rs = ps.executeQuery()
            if (rs.next()) {
                rs.close()
                ps.close()
                return Companion.json(Shortcut.UR, "The user $nickname has been registered.")
            }
            val registerTime = Date()
            val userId = idGenerator(registerTime, nickname)
            ps = conn.prepareStatement("insert into user (id, password, token, last_login_ip, last_login_time, email, log, nickname) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
            ps.setString(1, userId)
            ps.setString(2, password)
            ps.setString(3, Token.getToken(userId, LoginPlatform.MOBILE, "123456", registerTime, false))
            ps.setString(4, registerIP)
            ps.setString(5, StringUtil.getTime(registerTime))
            ps.setString(6, email)
            ps.setString(7, Log.register(registerTime, registerIP, nickname))
            ps.setString(8, nickname)
            ps.execute()
            return json(Shortcut.OK, "Ok, you have created a user, let's fun!")
        } catch (e: SQLException) {
            e.printStackTrace()
            return json(Shortcut.OTHER, "SQL ERROR")
        }
    }

    private fun idGenerator(registerTime: Date, nickname: String?) =
        ("${registerTime.time}$nickname${(10..99).random()}".hashCode() and Integer.MAX_VALUE).toString()

    companion object {
        fun checkNickname(nickname: String): Boolean{
            val conn = MySQLConn.mySQLConnection
            try {
                val ps = conn.prepareStatement("select * from user where nickname = ?")
                ps.setString(1, nickname)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    rs.close()
                    ps.close()
                    return true
                }
                rs.close()
                ps.close()
                return false
            } catch (e: SQLException) {
                e.printStackTrace()
                return true
            }
        }

        private fun json(shortcut: Shortcut, msg: String, data: HashMap<String, String>?=null): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            if(data!=null){map["data"] = JSONObject(data as Map<String, Any>?)}
            return map.toJSONString()
        }
    }
}
