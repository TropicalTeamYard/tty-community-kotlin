package servlet

import enums.Shortcut
import model.Message
import model.User
import util.CONF
import util.Value
import util.Value.fields
import util.file.FileUtil
import java.io.PrintWriter
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "api_public", urlPatterns = ["/api/public/*"])
class ApiPublic : HttpServlet() {
    private var ip: String = "0.0.0.0"
    private lateinit var out: PrintWriter

    private fun <T> Message<T>.write() {
        out.write(this.json())
    }

    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp!!.characterEncoding = "UTF-8"
        req!!.characterEncoding = "UTF-8"
        out = resp.writer
        ip = Value.getIP(req)
        val route = try {
            req.requestURI.substring(22)
        } catch (e: StringIndexOutOfBoundsException) {
            Message<Any>(Shortcut.AE, "invalid request").write()
            return
        }

        val fields = req.parameterMap.fields()

        when (route) {
            "info" -> {
                // http://localhost:8080/community/api/public/info?id=123
                val id = fields["id"]
                User.PublicInfo.get(id).write()
            }

            "portrait" -> {
                // http://localhost:8080/community/api/public/portrait?id=2008153477
                val id = fields["id"]
                val path = CONF.conf.portrait + "/" + User.getPortrait(id)
                FileUtil.writePicture2Response(resp, path, 1.0, 1.0)
                resp.reset()

            }

            // http://localhost:8080/community/api/public/test
            "test" -> test()

            else -> Message<Any>(Shortcut.AE, "invalid request").write()
        }
    }

    // checked
    private fun test() {
        out.write(CONF.root + "\n")
        out.write(CONF.conf.server)
    }
}