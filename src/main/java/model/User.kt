package model

import util.conn.MySQLConn
import java.sql.SQLException
import java.util.Date

class User {
    var id: String? = null
    var nickname: String? = null
    var password: String? = null
    var token: String? = null
    var lastLoginIP: String? = null
    var lastLoginTime: Date? = null
    var log: String? = null

    companion object {
        fun getNickname(id: String): String {
            val conn = MySQLConn.connection
            var nickname: String
            try {
                val ps = conn.prepareStatement("select nickname from user where id = ?")
                ps.setString(1, id)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    nickname = rs.getString("nickname")
                } else {
                    nickname = "`UNDEFINED`"
                }
                rs.close()
                ps.close()
            } catch (e: SQLException) {
                e.printStackTrace()
                nickname = "`UNDEFINED`"
            }

            return nickname
        }
    }
}
