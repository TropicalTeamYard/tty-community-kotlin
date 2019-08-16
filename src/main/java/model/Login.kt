package model

import com.alibaba.fastjson.JSONObject
import util.Value
import util.Value.json
import util.conn.MySQLConn
import util.enums.LoginPlatform
import util.enums.LoginType
import util.enums.Shortcut
import util.log.Log
import util.log.Token
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*
import kotlin.collections.HashMap

class Login(
    private var ip: String,
    private var platform: LoginPlatform
) {
    var id: String? = null
    var nickname: String? = null
    var password: String? = null
    var apiKey: String? = null
    var loginType: LoginType? = null
    private val loginTime = Date()

    fun submit(): String {
        val conn = MySQLConn.connection
        if (ip.isEmpty() || ip == "0.0.0.0"
            || loginType == null || (loginType == LoginType.ID && (id.isNullOrEmpty() || password.isNullOrEmpty()))
            || (loginType == LoginType.NICKNAME && (nickname.isNullOrEmpty()) || password.isNullOrEmpty())
            || (loginType == LoginType.THIRD_PARTY && (id.isNullOrEmpty() || apiKey.isNullOrEmpty()))
        ) {
            return json(Shortcut.AE, "arguments mismatch.")
        } else {
            try {
                var ps: PreparedStatement? = null
                when (loginType) {
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

                var rs = ps!!.executeQuery()

                if (rs.next()) {
                    val data = HashMap<String, String>()
                    id = rs.getString("id") ?: "null"
                    nickname = rs.getString("nickname")
                    data["id"] = id!!
                    data["nickname"] = nickname!!
                    data["email"] = rs.getString("email")
                    rs.close()
                    ps.close()
                    val token = Token.getToken(id!!, platform, "123456", loginTime, true)
                    ps =
                        conn.prepareStatement("update user set last_login_time = ?, last_login_ip = ?, token = ?  where id = ?")
                    ps.setString(1, Value.getTime(loginTime))
                    ps.setString(2, ip)
                    ps.setString(3, token)
                    ps.setString(4, id)
                    ps.executeUpdate()
                    Log.login(id!!, loginTime, ip, loginType!!, platform)
                    data["token"] = Value.getMD5(token) ?: "00000000000000000000000000000000"
                    ps.close()
                    return json(Shortcut.OK, "Ok, let's fun", data)
                } else {
                    rs.close()
                    ps.close()
                    when (loginType) {
                        LoginType.ID -> {
                            Log.loginFailed(id!!, loginTime, ip, loginType!!, platform)
                        }
                        LoginType.NICKNAME -> {
                            ps = conn.prepareStatement("select id from user  where nickname = ?")
                            ps.setString(1, nickname)
                            rs = ps.executeQuery()
                            if (rs.next()) {
                                id = rs.getString("id") ?: "null"
                                Log.loginFailed(id!!, loginTime, ip, loginType!!, platform)
                            }
                            rs.close()
                            ps.close()
                        }
                        LoginType.THIRD_PARTY -> {

                        }
                    }
                    return json(Shortcut.UPE, "wrong password.")
                }

            } catch (e: SQLException) {
                e.printStackTrace()
                return json(Shortcut.OTHER, "SQL ERROR")
            }

        }

    }
}
