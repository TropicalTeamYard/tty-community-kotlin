package servlet

import com.alibaba.fastjson.JSONObject
import model.*
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.servlet.ServletFileUpload
import util.LoginPlatform
import util.LoginType
import util.Shortcut
import util.StringUtil
import java.io.File
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

//TABLE USER

//# create database tty_community;
//# create table user(
//#     _id integer primary key auto_increment,
//#     id text not null,
//#     nickname text not null,
//#     token text not null,
//#     password text not null,
//#     last_login_ip text not null,
//#     last_login_time text not null,
//#     email text not null,
//#     log blob not null
//# );

@WebServlet(name = "api_user", urlPatterns = ["/api/user"])
class APIUser: HttpServlet() {
    private var reqIP: String = "0.0.0.0"
    private var method: ReqType = ReqType.Default
    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        reqIP = getIPAddr(req!!) ?:"0.0.0.0"
        resp?.writer?.write("API: APIUser\nIP: $reqIP\n")
        doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?){
        val out = resp!!.writer
        reqIP = getIPAddr(req!!) ?:"0.0.0.0"
        method = when(req.getParameter("method")){

            "login" -> {
                // http://localhost:8080/community/api/user?method=login&platform=web&login_type=id&id=720468899&password=9128639163198r91b
                val json = JSONObject()
                val platform: LoginPlatform = when(req.getParameter("platform")){
                    "mobile" -> LoginPlatform.MOBILE
                    "pc" -> LoginPlatform.PC
                    "web" -> LoginPlatform.WEB
                    "pad" -> LoginPlatform.PAD
                    else -> {
                        json["shortcut"] = "AE"
                        json["msg"] = "platform not allowed."
                        out.write(json.toJSONString())
                        return
                    }
                }
                val login = Login(reqIP, platform)
                when(req.getParameter("login_type")) {
                    "id" -> {
                        login.loginType = LoginType.ID
                        login.id = req.getParameter("id")
                        login.password = req.getParameter("password")
                        out.write(login.submit())
                    }
                    "nickname" -> {
                        login.loginType = LoginType.NICKNAME
                        login.nickname = req.getParameter("nickname")
                        login.password = req.getParameter("password")
                        out.write(login.submit())
                    }
                    "third_party" -> {
                        login.loginType = LoginType.THIRD_PARTY
                        login.id = req.getParameter("id")
                        login.apiKey = req.getParameter("api_key")
                        out.write(login.submit())
                    }
                    else -> {
                        json["shortcut"] = "AE"
                        json["msg"] = "invalid login_type."
                        out.write(json.toJSONString())
                        return
                    }
                }

                ReqType.Login
            }

            "auto_login" -> {
                // http://localhost:8080/community/api/user?method=auto_login&platform=pc&token=0F7B94AC09054FF9BBBF275340483BB9&id=720468899
                val json = JSONObject()
                val id = req.getParameter("id")
                val token = req.getParameter("token")
                val platform: LoginPlatform = when(req.getParameter("platform")){
                    "mobile" -> LoginPlatform.MOBILE
                    "pc" -> LoginPlatform.PC
                    "web" -> LoginPlatform.WEB
                    "pad" -> LoginPlatform.PAD
                    else -> {
                        json["shortcut"] = "AE"
                        json["msg"] = "platform not allowed."
                        out.write(json.toJSONString())
                        return
                    }
                }
                if(reqIP == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()){
                    out.write(AutoLogin.json(Shortcut.AE, "arguments mismatch."))
                    return
                }
                val auto = AutoLogin(reqIP, id, token, platform)
                out.write(auto.submit())
                ReqType.AutoLogin
            }

            "register" -> {
                // http://localhost:8080/community/api/user?method=register&nickname=wcf&password=123456&email=123@qq.com
                val nickname = req.getParameter("nickname")
                val email = req.getParameter("email")
                val password = req.getParameter("password")
                if(nickname.isNullOrEmpty() || email.isNullOrEmpty() || password.isNullOrEmpty() || reqIP=="0.0.0.0"){
                    out.write(Register.json(Shortcut.AE, "arguments mismatch."))
                    return
                }
                val result = Register(nickname, reqIP, email, password).submit()
                out.write(result)
                ReqType.Register
            }

            "check_name" -> {
                // http://localhost:8080/community/api/user?method=check_name&nickname=wcf
                val nickname = req.getParameter("nickname")
                val json = JSONObject()
                if(nickname == null || nickname.isEmpty()){
                    json["shortcut"] = "AE"
                    json["msg"] = "arguments mismatch."
                } else {
                    when(Register.checkNickname(req.getParameter("nickname"))){
                        false -> {
                            json["shortcut"] = "OK"
                            json["msg"] = "The nickname $nickname is not registered"
                        }
                        true -> {
                            json["shortcut"] = "UR"
                            json["msg"] = "The nickname $nickname has been registered"
                        }
                    }
                }
                out.write(json.toJSONString())
                ReqType.CheckName
            }

            "change_info" -> {
                // http://localhost:8080/community/api/user?method=change_info&token=2922598E94BCE57F9534909CC0404F97&id=720468899&nickname=wcf&email=1533144693@qq.com
                val id = req.getParameter("id")
                val token = req.getParameter("token")

                if(reqIP.isEmpty() || reqIP == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()){
                    out.write(StringUtil.json(Shortcut.AE, "argument mismatch."))
                    return
                }

                val changeUserInfo = ChangeUserInfo(id, token, reqIP)

                if (!req.getParameter("nickname").isNullOrEmpty()){
                    changeUserInfo.changedItem[UserInfoType.Nickname] = req.getParameter("nickname")
                }

                if (!req.getParameter("email").isNullOrEmpty()){
                    changeUserInfo.changedItem[UserInfoType.Email] = req.getParameter("email")
                }

                out.write(changeUserInfo.submit())

                ReqType.ChangeInfo
            }

            "change_detail_info" -> {

                val id = req.getParameter("id")
                val token = req.getParameter("token")

                if(reqIP.isEmpty() || reqIP == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()){
                    out.write(StringUtil.json(Shortcut.AE, "argument mismatch."))
                    return
                }

                val changeDetailInfo = ChangeDetailInfo(id, token, reqIP)

                if (!req.getParameter("signature").isNullOrEmpty()){
                    changeDetailInfo.changedItem[DetailInfoType.valueOf("signature")] = req.getParameter("signature")
                    changeDetailInfo.changedItem[DetailInfoType.Signature] = req.getParameter("signature")
                }


                ReqType.ChangeDetailInfo
            }

            "test" -> {
                // http://localhost:8080/community/api/user?method=test
                val jsonFile = File(this.servletContext.getRealPath("/conf/dir"))
                val conf = StringUtil.jsonFromFile(jsonFile)
                out.write(conf?.toJSONString()?:StringUtil.json(Shortcut.OTHER, "Failed"))
                ReqType.Test
            }

            "change_password" -> {
                // http://localhost:8080/community/api/user?method=change_password&id=720468899&old=123456&new=123456789
                val id = req.getParameter("id")
                val oldPassword = req.getParameter("old")
                val newPassword = req.getParameter("new")

                if(reqIP.isEmpty() || reqIP == "0.0.0.0" || id.isNullOrEmpty() || newPassword.isNullOrEmpty() || oldPassword.isNullOrEmpty()){
                    out.write(StringUtil.json(Shortcut.AE, "argument mismatch."))
                    return
                }

                out.write(ChangePassword(id, oldPassword, newPassword, reqIP).submit())

                ReqType.ChangePassword
            }

            else -> {
                out.write(StringUtil.json(Shortcut.AE, "invalid request."))
                ReqType.Default
            }
        }
    }

    enum class ReqType{
        Register, Login, AutoLogin, CheckName,
        ChangeInfo, ChangePassword, ChangeDetailInfo, UpdatePortrait,
        Test, Default
    }

    companion object {
        fun getIPAddr(request: HttpServletRequest): String? {
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
    }
}

@WebServlet(name = "api_public_user", urlPatterns = ["/api/public/user"])
class APIPublicUser: HttpServlet() {
    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?){
        val reqIP = APIUser.getIPAddr(req!!)?:"0.0.0.0"
        resp?.writer?.write("API: APIPublicUser\nIP: $reqIP\n")
        doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {

    }
}