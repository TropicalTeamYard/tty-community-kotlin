package orm

import util.CONF
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object Conn {
    private val SERVER = CONF.conf.server
    private val MySQLConnStr =
        "jdbc:mysql://$SERVER/tty_user_center?useUnicode=true&characterEncoding=utf8"
    private val USER = CONF.conf.user
    private val PASSWORD = CONF.conf.password


    private var conn: Connection? = null

    val connection: Connection
        get() {
            if (conn == null || (conn != null && conn!!.isClosed)) {
                try {
                    Class.forName("com.mysql.jdbc.Driver")
                    conn = DriverManager.getConnection(MySQLConnStr, USER, PASSWORD)
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                } catch (e: SQLException) {
                    e.printStackTrace()
                }

            }

            return conn!!
        }
}
