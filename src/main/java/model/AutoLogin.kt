package model

import com.alibaba.fastjson.JSONObject
import util.Value
import util.conn.MySQLConn
import util.enums.LoginPlatform
import util.enums.Shortcut
import util.log.Log
import java.sql.SQLException
import java.util.*
import kotlin.collections.HashMap

class AutoLogin(
    private val ip: String,
    private var id: String,
    private var token: String,
    private var platform: LoginPlatform
) {
    private val loginTime = Date()
    private val conn = MySQLConn.connection
    fun submit(): String {
        try {
            var ps = conn.prepareStatement("select * from user where id = ?")
            ps.setString(1, id)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val token = rs.getString("token")
                return if (Value.getMD5(token) == this.token) {
                    val data = HashMap<String, String>()
                    data["id"] = rs.getString("id")
                    data["nickname"] = rs.getString("nickname")
                    data["email"] = rs.getString("email")
                    rs.close()
                    ps.close()
                    ps = conn.prepareStatement("update user set last_login_ip = ?, last_login_time = ? where id = ?")
                    ps.setString(1, ip)
                    ps.setString(2, Value.getTime(loginTime))
                    ps.setString(3, id)
                    ps.executeUpdate()
                    ps.close()
                    Log.autoLogin(id, loginTime, ip, platform)
                    json(Shortcut.OK, "OK, let's fun", data)
                } else {
                    rs.close()
                    ps.close()
                    Log.autoLoginFailed(id, loginTime, ip, platform)
                    json(Shortcut.TE, "invalid token")
                }
            } else {
                ps.close()
                return json(Shortcut.UPE, "user $id not exist.")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return json(Shortcut.OTHER, "SQL ERROR")
        }

    }

    companion object {
        fun json(shortcut: Shortcut, msg: String, data: HashMap<String, String>? = null): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            if (data != null) {
                map["data"] = JSONObject(data as Map<String, Any>?)
            }
            return map.toJSONString()
        }
    }
}