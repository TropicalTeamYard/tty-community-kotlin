package servlet

import util.CONF
import util.Value.json
import util.enums.Shortcut
import util.parse.IP
import util.Value.getFields
import util.conn.MySQLConn
import java.io.PrintWriter
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@WebServlet(name = "api_topic", urlPatterns = ["/api/topic/*"])
class APITopic : HttpServlet() {
    private var ip: String = "0.0.0.0"
    private lateinit var out: PrintWriter


    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp?.characterEncoding = "utf-8"
        req?.characterEncoding = "utf-8"
        out = resp!!.writer
        ip = IP.getIPAddr(req!!)
        val route = try {
            req.requestURI.substring(21)
        } catch (e: StringIndexOutOfBoundsException) {
            out.write(json(Shortcut.AE, "invalid request"))
            return
        }

        val fields = req.parameterMap.getFields()

        when (route) {

            "create" -> {
                // http://47.102.200.155:8080/community/api/topic/create
                // id, token, name, parent, introduction

                val id = fields["id"]
                val token = fields["token"]
                val name = fields["name"]
                val parent = fields["parent"]
                val introduction = fields["introduction"]

                /**
                 * Only user group > 1 can create the topic, or the system will notify the message
                 * the board, and users can decide whether to create the topic, the user whose
                 * group > 3 can create the topic directly
                 *  Every topic must have a parent, except root topic ALL(id = "000000")
                 */

            }

            "find" -> {
                // http://47.102.200.155:8080/community/api/topic/find
            }

            "similar" -> {
                // http://47.102.200.155:8080/community/api/topic/similar
                //
            }

            "list" -> {
                // http://47.102.200.155:8080/community/api/topic/list
                // id
            }

            "follow" -> {
                // id, token, topic_id
            }

            "get_info" -> {

            }

            "change_info" -> {

            }

            "test" -> {
                // http://47.102.200.155:8080/community/api/topic/test
                test()
            }

            else -> {
                out.write(json(Shortcut.AE, "invalid request."))
            }
        }
    }

    private fun create(id: String, token: String, name: String, parent: String, introduction: String): Shortcut {
        TODO()
    }

    private fun find(name: String) {
        val conn = MySQLConn.connection
        val ps = conn.prepareStatement("select topic_id from topic where name = ?")
        ps.setString(1, name)
        val rs = ps.executeQuery()

    }

    private fun test() {
        out.write(CONF.root + "\n")
        out.write(CONF.conf.server)
    }

}