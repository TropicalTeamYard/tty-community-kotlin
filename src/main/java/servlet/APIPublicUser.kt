package servlet

import exception.Shortcut
import util.CONF
import util.Value
import util.conn.MySQLConn
import util.parse.IP
import java.io.FileInputStream
import java.io.PrintWriter
import java.sql.SQLException
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "api_public_user", urlPatterns = ["/api/public/user/*"])
class APIPublicUser : HttpServlet() {
    private var ip: String = "0.0.0.0"
    private lateinit var out: PrintWriter


    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp?.characterEncoding = "UTF-8"
        req?.characterEncoding = "UTF-8"
        out = resp!!.writer
        ip = IP.getIPAddr(req!!)
        val route = try {
            req.requestURI.substring(27)
        } catch (e: StringIndexOutOfBoundsException) {
            out.write(Value.json(Shortcut.AE, "invalid request"))
            return
        }

        when (route) {
            "info" -> {
                // http://localhost:8080/community/api/public/user/info?target=1285609993&items=personal_signature&items=nickname&items=follower&items=following&items=user_group
                getPublicInfo(req)
            }

            "portrait" -> {
                // http://localhost:8080/community/api/public/user/portrait?target=2008153477
                getPortrait(req, resp)
            }

            "test" -> {
                // http://localhost:8080/community/api/public/user/test
                test()
            }

            else -> {
                out.write(Value.json(Shortcut.AE, "invalid request"))
            }
        }
    }


    private fun test() {
        out.write(CONF.root + "\n")
        out.write(CONF.conf.server)
    }

    private fun getPortrait(req: HttpServletRequest?, resp: HttpServletResponse?) {
        val map = req!!.parameterMap
        val targetId = map["target"]?.get(0)
        if (targetId.isNullOrEmpty()) {
            out.write(Value.json(Shortcut.AE, "argument mismatch."))
            return
        }

        val conn = MySQLConn.connection
        try {
            val ps = conn.prepareStatement("select portrait from user_detail where id = ? limit 1")
            ps.setString(1, targetId)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val portrait = rs.getString("portrait")
                val path = CONF.conf.portrait + "/" + portrait
                val inputStream = FileInputStream(path)
                resp!!.reset()
                val os = resp.outputStream
                var len: Int
                val buffer = ByteArray(1024)
                do {
                    len = inputStream.read(buffer)
                    if (len == -1) {
                        break
                    }
                    os.write(buffer, 0, len)
                } while (true)

                os.close()
                inputStream.close()
                rs.close()
                ps.close()
            } else {
                out.write(Value.json(Shortcut.UNE, "the user $targetId have not been registered."))
                rs.close()
                ps.close()
            }


        } catch (e: SQLException) {
            e.printStackTrace()
            out.write(Value.json(Shortcut.OTHER, "SQL ERROR"))
        } catch (e: Exception) {
            e.printStackTrace()
            out.write(Value.json(Shortcut.OTHER, "UNKNOWN EXCEPTION"))
        }

    }

    private fun getPublicInfo(req: HttpServletRequest?) {
        val publicInfoKey = arrayOf("personal_signature", "following", "follower", "user_group")
        val infoKey = arrayOf("nickname")
        val map = req!!.parameterMap
        val targetId = map["target"]?.get(0)
        val items = map["items"]
        if (targetId.isNullOrEmpty() || items.isNullOrEmpty()) {
            out.write(Value.json(Shortcut.AE, "argument mismatch."))
            return
        }

        val conn = MySQLConn.connection
        val data = HashMap<String, String>()
        try {
            data["id"] = targetId
            var ps = conn.prepareStatement("select * from user_detail where id = ? limit 1")
            ps.setString(1, targetId)
            var rs = ps.executeQuery()
            if (rs.next()) {
                for (key in items) {
                    if (publicInfoKey.contains(key)) {
                        val value = rs.getString(key)
                        data[key] = value
                    }
                }
                rs.close()
                ps.close()
            } else {
                rs.close()
                ps.close()
                out.write(Value.json(Shortcut.UNE, "id $targetId have not been registered."))
                return
            }

            ps = conn.prepareStatement("select * from user where id = ?")
            ps.setString(1, targetId)
            rs = ps.executeQuery()
            if (rs.next()) {
                for (key in items) {
                    if (infoKey.contains(key)) {
                        val value = rs.getString(key)
                        data[key] = value
                    }
                }
                rs.close()
                ps.close()
            } else {
                rs.close()
                ps.close()
            }

            out.write(Value.json(Shortcut.OK, "the user info have been returned.", data))
        } catch (e: SQLException) {
            e.printStackTrace()
            out.write(Value.json(Shortcut.OTHER, "SQL ERROR"))
        }

    }

}