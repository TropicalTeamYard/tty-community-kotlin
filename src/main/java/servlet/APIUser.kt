package servlet

import com.alibaba.fastjson.JSONObject
import model.*
import util.CONF
import util.Value
import util.enums.LoginPlatform
import util.enums.LoginType
import util.enums.Shortcut
import util.enums.UserInfoType
import util.parse.IP
import util.parse.PortraitUpdater
import java.io.PrintWriter
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "api_user", urlPatterns = ["/api/user/*"])
class APIUser : HttpServlet() {
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
            req.requestURI.substring(20)
        } catch (e: StringIndexOutOfBoundsException) {
            out.write(Value.json(Shortcut.AE, "invalid request"))
            return
        }

        when (route) {
            "login" -> {
                // http://localhost:8080/community/api/user/login?platform=web&login_type=id&id=2008153477&password=123456789
                login(req)
            }

            "auto_login" -> {
                // http://localhost:8080/community/api/user/auto_login?platform=pc&token=040DC2D9B6B8069563E7DC10D53D1B0D&id=2008153477
                autoLogin(req)
            }

            "register" -> {
                // http://localhost:8080/community/api/user/register?nickname=wcf&password=123456&email=123@qq.com
                register(req)
            }

            "check_name" -> {
                // http://localhost:8080/community/api/user/check_name?nickname=wcf
                checkName(req)
            }

            "info" -> {
                // http://47.102.200.155:8080/community/api/user/info?id=2008153477&token=712AA3EB5A560EEFFDBF1638A7587767
                info(req)
            }

            "change_info" -> {
                // http://localhost:8080/community/api/user/change_info?token=2922598E94BCE57F9534909CC0404F97&id=720468899&nickname=wcf&email=1533144693@qq.com
                changeInfo(req)
            }

            "change_portrait" -> {
                // http://localhost:8080/community/api/user/change_portrait
                changePortrait(req)
            }

            "change_detail_info" -> {
                // http://localhost:8080/community/api/user/change_detail_info?id=1285609993&token=E0DC9F89E9C06F36072C27138833230B&params=personal_signature::helloworld&params=key::value
                changeDetailInfo(req)
            }

            "change_password" -> {
                // http://localhost:8080/community/api/user/change_password?id=2008153477&old=123456&new=123456789
                changePassword(req)
            }

            "test" -> {
                // http://localhost:8080/community/api/user/test
                test()
            }

            else -> {
                out.write(Value.json(Shortcut.AE, "invalid request."))
            }
        }
    }


    private fun test() {
        out.write(CONF.root + "\n")
        out.write(CONF.conf.server)
    }

    private fun changePassword(req: HttpServletRequest) {
        val id = req.getParameter("id")
        val oldPassword = req.getParameter("old")
        val newPassword = req.getParameter("new")

        if (ip.isEmpty() || ip == "0.0.0.0" || id.isNullOrEmpty() || newPassword.isNullOrEmpty() || oldPassword.isNullOrEmpty()) {
            out.write(Value.json(Shortcut.AE, "argument mismatch."))
            return
        }

        out.write(ChangePassword(id, oldPassword, newPassword, ip).submit())
    }

    private fun changeDetailInfo(req: HttpServletRequest) {
        val map = req.parameterMap
        val id = map["id"]?.get(0)
        val token = map["token"]?.get(0)

        if (ip.isEmpty() || ip == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()) {
            out.write(Value.json(Shortcut.AE, "argument mismatch."))
            return
        }

        val changeDetailInfo = ChangeDetailInfo(id, token, ip)

        val params = map["params"] ?: arrayOf()
        for (item in params) {
            try {
                val key = item.split("::")[0]
                val value = item.split("::")[1]
                changeDetailInfo.changedItem[key] = value
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
                continue
            }
        }
        if (changeDetailInfo.changedItem.size > 0) {
            out.write(changeDetailInfo.submit())
        } else {
            out.write(Value.json(Shortcut.OTHER, "Nothing changed"))
        }
    }

    private fun changePortrait(req: HttpServletRequest) {
        out.write(PortraitUpdater(req).submit())
    }

    private fun changeInfo(req: HttpServletRequest) {
        val id = req.getParameter("id")
        val token = req.getParameter("token")

        if (ip.isEmpty() || ip == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()) {
            out.write(Value.json(Shortcut.AE, "argument mismatch."))
            return
        }

        val changeUserInfo = ChangeUserInfo(id, token, ip)

        if (!req.getParameter("nickname").isNullOrEmpty()) {
            changeUserInfo.changedItem[UserInfoType.Nickname] = req.getParameter("nickname")
        }

        if (!req.getParameter("email").isNullOrEmpty()) {
            changeUserInfo.changedItem[UserInfoType.Email] = req.getParameter("email")
        }

        out.write(changeUserInfo.submit())
    }

    private fun info(req: HttpServletRequest) {
        val id = req.getParameter("id")
        val token = req.getParameter("token")
        if (id.isNullOrEmpty() || token.isNullOrEmpty() || ip.isEmpty() || ip == "0.0.0.0") {
            out.write(Value.json(Shortcut.AE, "arguments mismatch"))
            return
        }
        out.write(UserInfo.get(id, token))
    }

    private fun checkName(req: HttpServletRequest) {
        val nickname = req.getParameter("nickname")
        val json = JSONObject()
        if (nickname == null || nickname.isEmpty()) {
            json["shortcut"] = "AE"
            json["msg"] = "arguments mismatch."
        } else {
            when (Register.checkNickname(req.getParameter("nickname"))) {
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
    }

    private fun register(req: HttpServletRequest) {
        val nickname = req.getParameter("nickname")
        val email = req.getParameter("email")
        val password = req.getParameter("password")
        if (nickname.isNullOrEmpty() || email.isNullOrEmpty() || password.isNullOrEmpty() || ip == "0.0.0.0") {
            out.write(Register.json(Shortcut.AE, "arguments mismatch."))
            return
        }
        val result = Register(nickname, ip, email, password).submit()
        out.write(result)
    }

    private fun autoLogin(req: HttpServletRequest) {
        val json = JSONObject()
        val id = req.getParameter("id")
        val token = req.getParameter("token")
        val platform: LoginPlatform = when (req.getParameter("platform")) {
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
        if (ip == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()) {
            out.write(Value.json(Shortcut.AE, "arguments mismatch."))
            return
        }
        val auto = AutoLogin(ip, id, token, platform)
        out.write(auto.submit())
    }

    private fun login(req: HttpServletRequest) {
        val json = JSONObject()
        val platform: LoginPlatform = when (req.getParameter("platform")) {
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
        val login = Login(ip, platform)
        when (req.getParameter("login_type")) {
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
    }

}