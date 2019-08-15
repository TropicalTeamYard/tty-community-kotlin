package model

import com.alibaba.fastjson.JSONObject
import util.Value
import util.conn.MySQLConn
import util.enums.Shortcut
import util.enums.UserInfoType
import util.log.Log
import java.sql.SQLException
import java.util.*
import kotlin.collections.HashMap

@Suppress("ReplaceWithEnumMap")
class ChangeUserInfo(var id: String, var token: String, var ip: String) {
    var changedItem: HashMap<UserInfoType, String> = HashMap()
    private val date = Date()
    var succeedItem: HashMap<String, String> = HashMap()
    fun submit(): String {
        val conn = MySQLConn.connection
        try {
            var ps = conn.prepareStatement("select  * from user where id = ? limit 1")
            ps.setString(1, id)
            val rs = ps.executeQuery()
            if (rs.next()) {
                if (token == Value.getMD5(rs.getString("token"))) {
                    val nicknameBefore = rs.getString("nickname")
                    val emailBefore = rs.getString("email")
                    rs.close()
                    ps.close()

                    for (e in changedItem) {
                        when (e.key) {
                            UserInfoType.Email -> {
                                ps = conn.prepareStatement("update user set email = ? where id = ?")
                                ps.setString(1, e.value)
                                ps.setString(2, id)
                                ps.executeUpdate()
                                ps.close()
                                Log.changeUserInfo(id, date, ip, true, emailBefore, e.value, UserInfoType.Email)
                                succeedItem["email"] = e.value
                            }
                            UserInfoType.Nickname -> {
                                val isNicknameRegistered = Register.checkNickname(e.value)
                                if (!isNicknameRegistered) {
                                    ps = conn.prepareStatement("update user set nickname = ? where id = ?")
                                    ps.setString(1, e.value)
                                    ps.setString(2, id)
                                    ps.executeUpdate()
                                    ps.close()
                                    Log.changeUserInfo(
                                        id,
                                        date,
                                        ip,
                                        true,
                                        nicknameBefore,
                                        e.value,
                                        UserInfoType.Nickname
                                    )
                                    succeedItem["nickname"] = e.value
                                } else {
                                    Log.changeUserInfo(id, date, ip, false)
                                }

                            }
                            UserInfoType.Default -> {
                            }
                        }
                    }

                    return json(Shortcut.OK, "change user info succeed.", succeedItem)

                } else {
                    rs.close()
                    ps.close()
                    Log.changeUserInfo(id, date, ip, false)
                    return json(Shortcut.TE, "invalid token")
                }

            } else {
                rs.close()
                ps.close()
                Log.changeUserInfo(id, date, ip, false)
                return json(Shortcut.UNE, "user $id has not been registered.")
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

