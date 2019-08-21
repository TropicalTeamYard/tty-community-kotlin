package servlet

import enums.Shortcut
import model.Message
import model.User
import model.User.Companion.LoginPlatform.*
import model.User.Companion.LoginType.ID
import model.User.Companion.LoginType.NICKNAME
import model.User.Companion.exist
import util.CONF
import util.Value
import util.Value.fields
import util.Value.json
import java.io.PrintWriter
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "api_user", urlPatterns = ["/api/user/*"])
class ApiUser : HttpServlet() {
    private var ip: String = "0.0.0.0"
    private lateinit var out: PrintWriter

    private fun <T> Message<T>.write() {
        out.write(this.json())
    }

    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp!!.characterEncoding = "utf-8"
        req!!.characterEncoding = "utf-8"
        out = resp.writer
        ip = Value.getIP(req)
        val route = try {
            req.requestURI.substring(20)
        } catch (e: StringIndexOutOfBoundsException) {
            Message<Any>(Shortcut.AE, "invalid request").write()
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

            "change_password" -> {
                // http://localhost:8080/community/api/user/change_password?id=2008153477&old=123456&new=123456789
                changePassword(req)
            }

            "test" -> {
                // http://localhost:8080/community/api/user/test
                test()
            }

            else -> {
                out.write(json(Shortcut.AE, "invalid request."))
            }
        }
    }


    // checked
    private fun test() {
        out.write(CONF.root + "\n")
        out.write(CONF.conf.server)
    }

    // checked
    private fun changePassword(req: HttpServletRequest) {
        val fields = req.parameterMap.fields()
        val id = fields["id"]
        val oldPassword = fields["old"]
        val newPassword = fields["new"]
        if (ip == "0.0.0.0" || id.isNullOrEmpty() || newPassword.isNullOrEmpty() || oldPassword.isNullOrEmpty()) {
            Message<Any>(Shortcut.AE, "argument mismatch").write()
        } else {
            User.changePassword(id, oldPassword, newPassword, ip).write()
        }
    }

    // checked
    private fun changePortrait(req: HttpServletRequest) {
        User.changePortrait(req).write()
    }

    // checked
    private fun changeInfo(req: HttpServletRequest) {
        val fields = req.parameterMap.fields()
        val id = fields["id"]
        val token = fields["token"]
        val nickname = fields["nickname"]
        val email = fields["email"]
        val signature = fields["signature"]
        val school = fields["school"]
        if (ip == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()) {
            Message<Any>(Shortcut.AE, "argument mismatch").write()
        } else {
            val updater = User.Updater(id, token, ip)
            nickname?.let { updater.add(updater.nickname(it)) }
            email?.let { updater.add(updater.email(it)) }
            signature?.let { updater.add(updater.signature(it)) }
            school?.let { updater.add(updater.school(it)) }
            updater.result().write()
        }
    }

    // checked
    private fun info(req: HttpServletRequest) {
        val fields = req.parameterMap.fields()
        val id = fields["id"]
        val token = fields["token"]
        if (id.isNullOrEmpty() || token.isNullOrEmpty() || ip == "0.0.0.0") {
            Message<Any>(Shortcut.AE, "argument mismatch").write()
        } else {
            User.PrivateInfo.get(id, token).write()
        }
    }

    // checked
    private fun checkName(req: HttpServletRequest) {
        val fields = req.parameterMap.fields()
        val nickname = fields["nickname"]

        if (nickname.isNullOrEmpty()) {
            Message<Any>(Shortcut.AE, "argument mismatch").write()
        } else {
            when (nickname.exist()) {
                false -> {
                    Message<Any>(Shortcut.OK, "The nickname $nickname is not registered").write()
                }
                true -> {
                    Message<Any>(Shortcut.UR, "The nickname $nickname has been registered").write()
                }
            }
        }
    }

    // checked
    private fun register(req: HttpServletRequest) {
        val fields = req.parameterMap.fields()
        val nickname = fields["nickname"]
        val email = fields["email"]
        val password = fields["password"]
        if (nickname.isNullOrEmpty() || email.isNullOrEmpty() || password.isNullOrEmpty() || ip == "0.0.0.0") {
            Message<Any>(Shortcut.AE, "arhument mismatch").write()
        } else {
            User.register(nickname, ip, email, password).write()
        }
    }

    // checked
    private fun autoLogin(req: HttpServletRequest) {
        val fields = req.parameterMap.fields()
        val id = fields["id"]
        val token = fields["token"]
        val platform = when (fields["platform"]) {
            "mobile" -> MOBILE
            "pc" -> PC
            "web" -> WEB
            "pad" -> PAD
            else -> null
        }
        if (ip == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty() || platform == null) {
            Message<Any>(Shortcut.AE, "argument mismatch").write()
        } else {
            User.autoLogin(ip, id, token, platform).write()
        }
    }

    // checked
    private fun login(req: HttpServletRequest) {
        val fields = req.parameterMap.fields()
        val platform = when (fields["platform"]) {
            "mobile" -> MOBILE
            "pc" -> PC
            "web" -> WEB
            "pad" -> PAD
            else -> null
        }
        val type = when (fields["type"]) {
            "id" -> ID
            "nickname" -> NICKNAME
            else -> null
        }
        val id = fields["id"]
        val nickname = fields["nickname"]
        val password = fields["password"]

        if (ip == "0.0.0.0" || password.isNullOrBlank() || platform == null || type == null) {
            Message<Any>(Shortcut.AE, "argument mismatch").write()
        } else if ((type == ID && id.isNullOrEmpty()) || (type == NICKNAME && nickname.isNullOrEmpty())) {
            Message<Any>(Shortcut.AE, "account should not be null").write()
        } else {
            when (type) {
                ID -> {
                    if (id.isNullOrEmpty()) {
                        Message<Any>(Shortcut.AE, "id should not be null").write()
                    } else {
                        User.login(id, password, type, platform, ip).write()
                    }
                }
                NICKNAME ->  {
                    if (nickname.isNullOrEmpty()) {
                        Message<Any>(Shortcut.AE, "nickname should not be null").write()
                    } else {
                        User.login(nickname, password, type, platform, ip).write()
                    }
                }
            }
        }
    }

}