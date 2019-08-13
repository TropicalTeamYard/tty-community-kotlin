package util.conn

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object MySQLConn {
    private const val SERVER_HOST = "47.102.200.155:3306"
    private const val MySQLConnStr = "jdbc:mysql://$SERVER_HOST/tty_community?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true"
    private var conn: Connection? = null

    val connection: Connection
        get() {
            if (conn == null || (conn != null && conn!!.isClosed)) {
                try {
                    Class.forName("com.mysql.jdbc.Driver")
                    val user = "root"
                    val password = "Mi@feifei658146603"
                    conn = DriverManager.getConnection(MySQLConnStr, user, password)
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                } catch (e: SQLException) {
                    e.printStackTrace()
                }

            }

            return conn!!
        }
}
