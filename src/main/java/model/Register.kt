package model

import com.alibaba.fastjson.JSONObject
import util.*
import java.sql.SQLException
import java.util.*


class Register(
    private val nickname: String,
    private val registerIP: String,
    private val email: String,
    private val password: String
) {
    fun submit(): String {
        //TODO CHECK WHETHER USER INFO IS VALID
        val conn = MySQLConn.connection
        try {
            var ps = conn.prepareStatement("select * from user where nickname = ?")
            ps.setString(1, nickname)
            var rs = ps.executeQuery()
            if (rs.next()) {
                rs.close()
                ps.close()
                return json(Shortcut.UR, "The user $nickname has been registered.")
            }
            rs.close()
            ps.close()
            val registerTime = Date()
            val userId = idGenerator(registerTime, nickname)
            ps = conn.prepareStatement("insert into user (id, nickname, password, token, last_login_ip, last_login_time, email) VALUES (?, ?, ?, ?, ?, ?, ?)")
            ps.setString(1, userId)
            ps.setString(2, nickname)
            ps.setString(3, password)
            ps.setString(4, Token.getToken(userId, LoginPlatform.MOBILE, "123456", registerTime, false))
            ps.setString(5, registerIP)
            ps.setString(6, StringUtil.getTime(registerTime))
            ps.setString(7, email)
            ps.execute()
            ps.close()
            ps = conn.prepareStatement("insert into user_detail (id, portrait, follower, following, personal_signature, account_status, exp, log) values (?, ?, ?, ?, ?, ?, ?, ?)")
            ps.setString(1, userId)
            ps.setString(2, "default")
            ps.setString(3, "000000")
            ps.setString(4, "000000")
            ps.setString(5, "no signature yet.")
            ps.setString(6, "normal::0")
            ps.setInt(7, 20)
            ps.setString(8, "init\n")
            ps.execute()
            ps.close()
            ps = conn.prepareStatement("select id from user where nickname = ? limit 1")
            ps.setString(1, nickname)
            rs = ps.executeQuery()
            val id: String
            return if(rs.next()){
                id = rs.getString("id")
                Log.register(id, registerTime, registerIP, nickname)
                rs.close()
                ps.close()
                json(Shortcut.OK, "Ok, you have created a user, let's fun!")
            } else {
                rs.close()
                ps.close()
                json(Shortcut.OTHER, "sorry, you have failed to register the account $nickname")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return json(Shortcut.OTHER, "SQL ERROR")
        }
    }

    private fun idGenerator(registerTime: Date, nickname: String?) =
        ("${registerTime.time}$nickname${(10..99).random()}".hashCode() and Integer.MAX_VALUE).toString()

    companion object {
        fun checkNickname(nickname: String): Boolean{
            val conn = MySQLConn.connection
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

        fun json(shortcut: Shortcut, msg: String, data: HashMap<String, String>?=null): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            if(data!=null){map["data"] = JSONObject(data as Map<String, Any>?)}
            return map.toJSONString()
        }
    }
}
