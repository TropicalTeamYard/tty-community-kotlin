package model

import util.Value
import util.conn.MySQLConn
import util.enums.Shortcut
import util.log.Log
import java.sql.SQLException
import java.util.*
import kotlin.collections.HashMap

class ChangeDetailInfo(private var id: String, private var token: String, private var ip: String) {
    var changedItem = HashMap<String, String>()
    var successItem = HashMap<String, String>()
    fun submit(): String {
        val conn = MySQLConn.connection
        val date = Date()
        try {
            var ps = conn.prepareStatement("select token from user where id = ? limit 1")
            ps.setString(1, id)
            var rs = ps.executeQuery()
            if (rs.next() && Value.getMD5(rs.getString("token")) == token) {
                rs.close()
                ps.close()
                ps = conn.prepareStatement("select * from user_detail where id = ? limit 1")
                ps.setString(1, id)
                rs = ps.executeQuery()
                rs.next()
                for (item in changedItem) {
                    if (DetailInfoItems.items.contains(item.key)) {
                        val valueBefore = rs.getString(item.key)
                        val ps1 = conn.prepareStatement("update user_detail set ${item.key} = ? where id = ?")
                        ps1.setString(1, item.value)
                        ps1.setString(2, id)
                        ps1.executeUpdate()
                        ps1.close()
                        Log.changeUserDetailInfo(id, date, ip, true, valueBefore, item.value, item.key)
                        successItem[item.key] = item.value
                    } else {
                        Log.changeUserDetailInfo(id, date, ip, false, target = item.key)
                    }
                }

                rs.close()
                ps.close()
                return ChangeUserInfo.json(
                    Shortcut.OK,
                    "change user info succeed",
                    successItem
                )

            } else {
                rs.close()
                ps.close()
                return ChangeUserInfo.json(Shortcut.TE, "invalid token.")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return ChangeUserInfo.json(Shortcut.OTHER, "SQL ERROR")
        }
    }
}