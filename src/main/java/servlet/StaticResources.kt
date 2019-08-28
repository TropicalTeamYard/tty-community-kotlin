package servlet

import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "static", urlPatterns = ["/conf/*"])
class StaticResources: HttpServlet() {
    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        super.doPost(req, resp)
    }

    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        super.doGet(req, resp)
    }
}