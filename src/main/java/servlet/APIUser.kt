package servlet

import exception.Message
import exception.Shortcut
import exception.ShortcutThrowable
import model.User
import model.User.Companion.LoginPlatform
import model.User.Companion.LoginPlatform.*
import model.User.Companion.LoginType.ID
import model.User.Companion.LoginType.NICKNAME
import model.User.Companion.exist
import util.CONF
import util.Value.getFields
import util.Value.json
import util.parse.IP
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
            out.write(json(Shortcut.AE, "invalid request"))
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


    private fun test() {
        out.write(CONF.root + "\n")
        out.write(CONF.conf.server)
    }

    private fun changePassword(req: HttpServletRequest) {
        val fields = req.parameterMap.getFields()
        val id = fields["id"]
        val oldPassword = fields["old"]
        val newPassword = fields["new"]


        try {
            if (ip == "0.0.0.0" || id.isNullOrEmpty() || newPassword.isNullOrEmpty() || oldPassword.isNullOrEmpty()) {
                throw ShortcutThrowable.AE()
            } else {
                throw User.changePassword(id, oldPassword, newPassword, ip)
            }
        } catch (info: ShortcutThrowable) {
            out.write(info.json())
        }


    }

    private fun changePortrait(req: HttpServletRequest) {
        try {
            if (User.changePortrait(req)) {
                out.write(Message(Shortcut.OK, "portrait changed", null).json())
            }
        } catch (info: ShortcutThrowable) {
            out.write(info.json())
        }
    }

    private fun changeInfo(req: HttpServletRequest) {
        val fields = req.parameterMap.getFields()
        val id = fields["id"]
        val token = fields["token"]
        val nickname = fields["nickname"]
        val email = fields["email"]
        val signature = fields["signature"]
        val school = fields["school"]
        try {
            if (ip == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()) {
                throw ShortcutThrowable.AE()
            }
            val updater = User.Updater(id, token, ip)
            nickname?.let { updater.add(updater.nickname(it)) }
            email?.let { updater.add(updater.email(it)) }
            signature?.let { updater.add(updater.signature(it)) }
            school?.let { updater.add(updater.school(it)) }
            val list = updater.result()
            throw ShortcutThrowable.OK("the result returned", list)
        } catch (info: ShortcutThrowable) {
            out.write(info.json())
        } catch (e: Exception) {
            e.printStackTrace()
            out.write(ShortcutThrowable.OTHER().json())
        }
    }

    private fun info(req: HttpServletRequest) {
        val fields = req.parameterMap.getFields()
        val id = fields["id"]
        val token = fields["token"]
        try {
            if (id.isNullOrEmpty() || token.isNullOrEmpty() || ip == "0.0.0.0") {
                throw ShortcutThrowable.AE()
            }
            val info = User.PrivateInfo.get(id, token)
            throw ShortcutThrowable.OK("success get info", info)
        } catch (info: ShortcutThrowable) {
            info.printStackTrace()
            out.write(info.json())
        }
    }

    private fun checkName(req: HttpServletRequest) {
        val fields = req.parameterMap.getFields()
        val nickname = fields["nickname"]

        out.write(
            if (nickname.isNullOrEmpty()) {
                ShortcutThrowable.AE().json()
            } else {
                when (nickname.exist()) {
                    false -> {
                        ShortcutThrowable.OK("The nickname $nickname is not registered").json()
                    }
                    true -> {
                        ShortcutThrowable.UR("The nickname $nickname has been registered").json()
                    }
                }
            }
        )
    }

    private fun register(req: HttpServletRequest) {
        val fields = req.parameterMap.getFields()
        val nickname = fields["nickname"]
        val email = fields["email"]
        val password = fields["password"]
        try {
            if (nickname.isNullOrEmpty() || email.isNullOrEmpty() || password.isNullOrEmpty() || ip == "0.0.0.0") {
                throw ShortcutThrowable.AE()
            } else {
                throw User.register(nickname, ip, email, password)
            }
        } catch (info: ShortcutThrowable) {
            out.write(info.json())
        }
    }

    private fun autoLogin(req: HttpServletRequest) {
        val fields = req.parameterMap.getFields()
        try {
            val id = fields["id"]
            val token = fields["token"]
            val platform: LoginPlatform = when (fields["platform"]) {
                "mobile" -> MOBILE
                "pc" -> PC
                "web" -> WEB
                "pad" -> PAD
                else -> {
                    throw ShortcutThrowable.AE("platform not allowed")
                }
            }
            if (ip == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()) {
                throw ShortcutThrowable.AE()
            }

            throw User.autoLogin(ip, id, token, platform)
        } catch (info: ShortcutThrowable) {
            out.write(info.json())
        }
    }

    private fun login(req: HttpServletRequest) {
        val fields = req.parameterMap.getFields()
        try {
            val platform: LoginPlatform = when (fields["platform"]) {
                "mobile" -> MOBILE
                "pc" -> PC
                "web" -> WEB
                "pad" -> PAD
                else -> throw ShortcutThrowable.AE("platform not allowed")
            }
            val type = when (fields["type"]) {
                "id" -> ID
                "nickname" -> NICKNAME
                else -> throw ShortcutThrowable.AE("type not allowed")
            }

            val id = fields["id"]
            val nickname = fields["nickname"]
            val password = fields["password"]

            if (ip == "0.0.0.0" || password.isNullOrBlank()) {
                throw ShortcutThrowable.AE()
            }

            if ((type == ID && id.isNullOrEmpty()) || (type == NICKNAME && nickname.isNullOrEmpty())) {
                throw ShortcutThrowable.AE("account should not be null")
            }

            when (type) {
                ID -> {
                    throw id?.let { User.login(it, password, type, platform, ip) } ?: throw ShortcutThrowable.UNE()
                }
                NICKNAME -> {
                    throw nickname?.let { User.login(it, password, type, platform, ip) }
                        ?: throw ShortcutThrowable.UNE()
                }
            }
        } catch (info: ShortcutThrowable) {
            out.write(info.json())
        }
    }

}