package model

import util.Value.json
import util.conn.MySQLConn
import util.enums.LoginPlatform
import util.enums.Shortcut
import util.log.Log
import util.log.Token
import java.sql.SQLException
import java.util.*

class ChangePassword(
    private var id: String,
    private var oldPassword: String,
    private var newPassword: String,
    var ip: String
) {

    private val conn = MySQLConn.connection
    private val date = Date()

    fun submit(): String {
        try {
            var ps = conn.prepareStatement("select * from user where id = ? limit 1")
            ps.setString(1, id)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val token =
                    Token.getToken(id, LoginPlatform.MOBILE, "7894556", Date(), false)
                val password = rs.getString("password")
                if (password == oldPassword) {
                    rs.close()
                    ps.close()
                    ps = conn.prepareStatement("update user set password = ?, token = ? where id = ?")
                    ps.setString(1, newPassword)
                    ps.setString(2, token)
                    ps.setString(3, id)
                    ps.executeUpdate()
                    ps.close()

                    Log.changePassword(id, date, ip, true)
                    return json(Shortcut.OK, "change password succeed.")
                } else {
                    rs.close()
                    ps.close()
                    Log.changePassword(id, date, ip, false)
                    return json(Shortcut.UPE, "wrong password.")
                }
            } else {
                rs.close()
                ps.close()
                return json(Shortcut.UNE, "user $id has not been registered.")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return json(Shortcut.OTHER, "SQL ERROR")
        }
    }

}