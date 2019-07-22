package servlet

import com.alibaba.fastjson.JSONObject
import model.RegisterInfo
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "api", urlPatterns = ["/api/user"])
class API: HttpServlet() {
    private var reqIP: String = "0.0.0.0"
    private var method: ReqType = ReqType.Default
    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        reqIP = getIpAddr(req!!)?:"0.0.0.0"
        resp?.writer?.write("IP: $reqIP\n")
        doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?){
        val out = resp!!.writer
        method = when(req?.getParameter("method")){
            "login" -> {ReqType.Login}
            "auto" -> {ReqType.AutoLogin}
            "register" -> {
                reqIP = getIpAddr(req)?:"0.0.0.0"
                val result = RegisterInfo(req.getParameter("nickname"), reqIP, req.getParameter("email"), req.getParameter("password")).submit()
                out.write(result)
                ReqType.Register
            }
            else -> {
                out.write("{\"status\":\"error\", \"code\":\"-100\", \"msg\":\"invalid request\"}")
                ReqType.Default
            }
        }
    }

    private fun getIpAddr(request: HttpServletRequest): String? {
        var ip: String? = request.getHeader("x-forwarded-for")
        if (ip != null && ip.isNotEmpty() && !"unknown".equals(ip, ignoreCase = true)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.contains(",")) {
                ip = ip.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            }
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_CLIENT_IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("X-Real-IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }
        return ip
    }

    enum class ReqType{
        Register, Login, AutoLogin, Default
    }
}