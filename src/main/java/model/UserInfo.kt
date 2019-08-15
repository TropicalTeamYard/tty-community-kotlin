package model

import com.alibaba.fastjson.JSONObject
import util.Value
import util.conn.MySQLConn
import util.enums.Shortcut
import java.sql.SQLException

class UserInfo(
    val id: String,
    val nickname: String,
    val email: String,
    val portrait: String,
    val signature: String,
    val userGroup: String,
    val exp: String
) {
    companion object {
        fun get(id: String, token: String): String {
            try {
                val conn = MySQLConn.connection
                val ps = conn.prepareStatement("select id, nickname, email, token from user where id = ? limit 1")
                ps.setString(1, id)
                val rs = ps.executeQuery()
                if (rs.next() && token == Value.getMD5(rs.getString("token"))) {
                    val nickname: String = rs.getString("nickname")
                    val email: String = rs.getString("email")
                    rs.close()
                    ps.close()
                    val ps1 =
                        conn.prepareStatement("select portrait, personal_signature, user_group, exp from user_detail where id = ? limit 1")
                    ps1.setString(1, id)
                    val rs1 = ps1.executeQuery()
                    if (rs1.next()) {
                        val portrait = rs1.getString("portrait")
                        val signature = rs1.getString("personal_signature")
                        val userGroup = rs1.getString("user_group")
                        val exp = "${rs1.getInt("exp")}"
                        rs1.close()
                        ps1.close()
                        val userInfo = UserInfo(id, nickname, email, portrait, signature, userGroup, exp)
                        val json = JSONObject()
                        json["shortcut"] = "OK"
                        json["msg"] = "return user info successfully"
                        json["data"] = userInfo
                        return json.toJSONString()
                    } else {
                        rs1.close()
                        ps1.close()
                        return Value.json(Shortcut.UNE, "user $id not found (step 2)")
                    }

                } else {
                    rs.close()
                    ps.close()
                    return Value.json(Shortcut.TE, "invalid id or token")
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                return Value.json(Shortcut.OTHER, "SQL ERROR")
            }

        }
    }
}