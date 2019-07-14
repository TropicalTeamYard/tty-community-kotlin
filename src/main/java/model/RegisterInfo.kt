package model

import util.MySQLConn
import util.StringUtil
import java.lang.Exception
import java.sql.SQLException
import java.util.*


class RegisterInfo(
    private val nickname: String,
    private val registerIP: String,
    private val email: String,
    private val password: String
) {
    fun submit(): HashMap<String, String> {
        //TODO CHECK WHETHER USER INFO IS VALID
        val result = HashMap<String, String>()
        val conn = MySQLConn.mySQLConnection
        try {
            if(nickname.isEmpty()||registerIP.isEmpty()||registerIP=="0.0.0.0"||email.isEmpty()||password.isEmpty()){
                result["status"] = "failed"
                result["code"] = "104"
                result["msg"] = "error : Invalid Parameter"
            }

            var ps = conn.prepareStatement("select * from user where nickname = ?")
            ps.setString(1, nickname)
            val rs = ps.executeQuery()
            if (rs.next()) {
                result["status"] = "failed"
                result["code"] = "101"
                result["msg"] = "error : Nickname Conflict"
                rs.close()
                ps.close()
                return result
            }
            val registerTime = Date()
            val userId = "${registerTime.time}$nickname${(10..99).random()}".hashCode().toString()
            ps = conn.prepareStatement("insert into user (id, password, token, last_login_ip, last_login_time, email, log, nickname) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
            ps.setString(1, userId)
            ps.setString(2, password)
            ps.setString(3, "{\"time\":\"" + StringUtil.getTime(registerTime) + "\"}")
            ps.setString(4, registerIP)
            ps.setString(5, StringUtil.getTime(registerTime))
            ps.setString(6, email)
            ps.setString(7, Log.register(registerTime, registerIP, nickname))
            ps.setString(8, nickname)
            ps.execute()
            result["status"] = "success"
            result["code"] = "100"
            result["msg"] = "id : $userId"
        } catch (e: SQLException) {
            result["status"] = "failed"
            result["code"] = "102"
            result["msg"] = "error : SQL"
            e.printStackTrace()
        }

        return result
    }
}
