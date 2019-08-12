package servlet

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import model.*
import util.*
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.sql.SQLException
import java.sql.Timestamp
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@WebServlet(name = "api_user", urlPatterns = ["/api/user/*"])
class APIUser: HttpServlet() {
    private var reqIP: String = "0.0.0.0"
    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        reqIP = getIPAddr(req!!) ?:"0.0.0.0"
        resp?.writer?.write("API: APIUser\nIP: $reqIP\n")
        doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?){
        resp?.characterEncoding = "utf-8"
        req?.characterEncoding = "utf-8"
        val out = resp!!.writer
        reqIP = getIPAddr(req!!) ?:"0.0.0.0"
        val route = try {
            req.requestURI.substring(20)
        } catch (e: StringIndexOutOfBoundsException) {
            out.write(APIPublicUser.json(Shortcut.AE, "invalid request"))
            return
        }

        when(route){
            "login" -> {
                // http://localhost:8080/community/api/user/login?platform=web&login_type=id&id=2008153477&password=123456789
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
            }

            "auto_login" -> {
                // http://localhost:8080/community/api/user/auto_login?platform=pc&token=040DC2D9B6B8069563E7DC10D53D1B0D&id=2008153477
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
            }

            "register" -> {
                // http://localhost:8080/community/api/user/register?nickname=wcf&password=123456&email=123@qq.com
                val nickname = req.getParameter("nickname")
                val email = req.getParameter("email")
                val password = req.getParameter("password")
                if(nickname.isNullOrEmpty() || email.isNullOrEmpty() || password.isNullOrEmpty() || reqIP=="0.0.0.0"){
                    out.write(Register.json(Shortcut.AE, "arguments mismatch."))
                    return
                }
                val result = Register(nickname, reqIP, email, password).submit()
                out.write(result)
            }

            "check_name" -> {
                // http://localhost:8080/community/api/user/check_name?nickname=wcf
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
            }

            "change_info" -> {
                // http://localhost:8080/community/api/user/change_info?token=2922598E94BCE57F9534909CC0404F97&id=720468899&nickname=wcf&email=1533144693@qq.com
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
            }

            "change_detail_info" -> {
                // http://localhost:8080/community/api/user/change_detail_info?id=1285609993&token=E0DC9F89E9C06F36072C27138833230B&params=personal_signature::helloworld&params=key::value
                val map = req.parameterMap
                val id = map["id"]?.get(0)
                val token = map["token"]?.get(0)

                if(reqIP.isEmpty() || reqIP == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()){
                    out.write(StringUtil.json(Shortcut.AE, "argument mismatch."))
                    return
                }

                val changeDetailInfo = ChangeDetailInfo(id, token, reqIP)

                val params = map["params"]?: arrayOf()
                for(item in params) {
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
                    out.write(StringUtil.json(Shortcut.OTHER, "Nothing changed"))
                }
            }

            "change_password" -> {
                // http://localhost:8080/community/api/user/change_password?id=2008153477&old=123456&new=123456789
                val id = req.getParameter("id")
                val oldPassword = req.getParameter("old")
                val newPassword = req.getParameter("new")

                if(reqIP.isEmpty() || reqIP == "0.0.0.0" || id.isNullOrEmpty() || newPassword.isNullOrEmpty() || oldPassword.isNullOrEmpty()){
                    out.write(StringUtil.json(Shortcut.AE, "argument mismatch."))
                    return
                }

                out.write(ChangePassword(id, oldPassword, newPassword, reqIP).submit())
            }

            "test" -> {
                // http://localhost:8080/community/api/user/test
                val jsonFile = File(this.servletContext.getRealPath("/conf/dir"))
                val conf = FileReadUtil.readJson(jsonFile)
                out.write(conf.toJSONString() ?:StringUtil.json(Shortcut.OTHER, "Failed"))
            }

            else -> {
                out.write(StringUtil.json(Shortcut.AE, "invalid request."))
            }
        }
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

@WebServlet(name = "api_public_user", urlPatterns = ["/api/public/user/*"])
class APIPublicUser: HttpServlet() {
    private var ip: String = "0.0.0.0"
    private lateinit var out: PrintWriter

    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?){
        val reqIP = APIUser.getIPAddr(req!!)?:"0.0.0.0"
        resp?.writer?.write("API: APIPublicUser\nIP: $reqIP\n")
        doPost(req, resp)
    }


    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp?.characterEncoding = "UTF-8"
        req?.characterEncoding = "UTF-8"
        out = resp!!.writer
        ip = APIUser.getIPAddr(req!!) ?:"0.0.0.0"
        val route = try {
            req.requestURI.substring(27)
        } catch (e: StringIndexOutOfBoundsException) {
            out.write(json(Shortcut.AE, "invalid request"))
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
                test()
            }

            else -> {
                out.write(json(Shortcut.AE, "invalid request"))
            }
        }
    }


    private fun test() {
        val jsonFile = File(this.servletContext.getRealPath("/conf/dir"))
        val conf = FileReadUtil.readJson(jsonFile)
        out.write(conf.toJSONString() ?:StringUtil.json(Shortcut.OTHER, "Failed"))
    }


    private fun getPortrait(req: HttpServletRequest?, resp: HttpServletResponse?) {
        val map = req!!.parameterMap
        val targetId = map["target"]?.get(0)
        if (targetId.isNullOrEmpty()) {
            out.write(json(Shortcut.AE, "argument mismatch."))
            return
        }

        val conn = MySQLConn.connection
        try {
            val ps = conn.prepareStatement("select portrait from user_detail where id = ? limit 1")
            ps.setString(1, targetId)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val portrait = rs.getString("portrait")
                val jsonFile = File(this.servletContext.getRealPath("/conf/dir"))
                val conf = FileReadUtil.readJson(jsonFile)
                val path = conf.getString("root") +"\\${conf.getString("portrait")}\\$portrait"
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
                out.write(json(Shortcut.UNE, "the user $targetId have not been registered."))
                rs.close()
                ps.close()
            }


        } catch (e: SQLException) {
            e.printStackTrace()
            out.write(json(Shortcut.OTHER, "SQL ERROR"))
        } catch (e: Exception) {
            e.printStackTrace()
            out.write(json(Shortcut.OTHER, "UNKNOWN EXCEPTION"))
        }

    }

    private fun getPublicInfo(req: HttpServletRequest?) {
        val publicInfoKey = arrayOf("personal_signature", "following", "follower", "user_group")
        val infoKey = arrayOf("nickname")
        val map = req!!.parameterMap
        val targetId = map["target"]?.get(0)
        val items = map["items"]
        if (targetId.isNullOrEmpty() || items.isNullOrEmpty()) {
            out.write(json(Shortcut.AE, "argument mismatch."))
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
                out.write(json(Shortcut.UNE, "id $targetId have not been registered."))
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

            out.write(json(Shortcut.OK, "the user info have been returned.", data))
        } catch (e: SQLException) {
            e.printStackTrace()
            out.write(json(Shortcut.OTHER, "SQL ERROR"))
        }

    }

    companion object {
        fun json(shortcut: Shortcut, msg: String, data: HashMap<String, String>? = null): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            if(data != null){
                map["data"] = JSONObject(data as Map<String, Any>?)
            }
            return map.toJSONString()
        }
    }

}

@WebServlet(name = "api_blog", urlPatterns = ["/api/blog/*"])
class APIBlog: HttpServlet() {
    private var ip: String = "0.0.0.0"
    private lateinit var out: PrintWriter


    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?){
//        val reqIP = APIUser.getIPAddr(req!!)?:"0.0.0.0"
//        resp?.writer?.write("API: APIBlog\nIP: $reqIP\n")
        doPost(req, resp)
    }


    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp?.characterEncoding = "UTF-8"
        req?.characterEncoding = "UTF-8"
        out = resp!!.writer
        ip = APIUser.getIPAddr(req!!) ?:"0.0.0.0"
        val route = try {
            req.requestURI.substring(20)
        } catch (e: StringIndexOutOfBoundsException) {
            out.write(json(Shortcut.AE, "invalid request"))
            return
        }

        when (route) {
            "test" -> {
                // http://localhost:8080/community/api/blog/test
                test()
                return
            }

            "create" -> {
                create(req)
                return
            }

            "get" -> {
                // http://localhost:8080/community/api/blog/get?id=746235507&type=json
                getBlog(req)
                return
            }


            "list" -> {
                // http://localhost:8080/community/api/blog/list?type=id&to=1293637237&count=8 # id 为 `to` 之前日期的 count 条记录 &tag=?
                // http://localhost:8080/community/api/blog/list?type=id&from=1293637237&count=8 # id 为 `from` 之后日期的 count 条记录 &tag=?
                // http://localhost:8080/community/api/blog/list?type=time&date=2019/8/25-03:24:52&count=2 # date 及之前日期的 count 条记录 &tag=?
                getBlogList(req)
                return
            }

            "picture" -> {
                // http://localhost:8080/community/api/blog/picture?id=700642438&index=0
                getBlogPic(req, resp)
                return
            }

            else -> {
                out.write(json(Shortcut.AE, "invalid request"))
                return
            }
        }
    }

    private fun create(req: HttpServletRequest?) {
        val date = java.util.Date()
        val time = Timestamp(date.time)
        val params: HashMap<String, String>
        val reqMaps = RequestPhrase(req!!)
        params = reqMaps.getField()

        val id = params["id"]
        val token = params["token"]
        val type = params["type"]
        val title = params["title"]
        val introduction = params["introduction"]
        val content = params["content"]
        val tag = params["tag"]
        val filesCount = params["file_count"]

        if (ip == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty() || type.isNullOrEmpty() || title.isNullOrEmpty() || introduction.isNullOrEmpty() || content.isNullOrEmpty() || tag.isNullOrEmpty() || filesCount.isNullOrEmpty()) {
            out.write(json(Shortcut.AE, "argument mismatch."))
            return
        }

        val blogId = ("$ip$id$token${date.time}${(1000..9999).random()}".hashCode() and Integer.MAX_VALUE).toString()

        try {
            val conn = MySQLConn.connection
            var ps = conn.prepareStatement("select * from user where id = ? limit 1")
            ps.setString(1, id)
            var rs = ps.executeQuery()
            if (rs.next() && token == StringUtil.getMD5(rs.getString("token"))) {
                rs.close()
                ps.close()
                ps = conn.prepareStatement("insert into blog (blog_id, author_id, title, introduction, content, tag, last_edit_time, last_active_time, status, data, log, comment, likes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                ps.setString(1, blogId)
                ps.setString(2, id)
                ps.setString(3, title)
                ps.setString(4, introduction)
                ps.setString(5, content)
                ps.setString(6, tag)
                ps.setTimestamp(7, time) // last_edit_time
                ps.setTimestamp(8, time) // last_active_time
                ps.setString(9, "normal") // status
                ps.setString(10, "files:$filesCount") // data
                ps.setString(11, "init\n") // log
                ps.setString(12, "") // comment
                ps.setString(13, "") // likes
                ps.execute()
                ps.close()
                ps = conn.prepareStatement("select * from blog where blog_id = ? limit 1")
                ps.setString(1, blogId)
                rs = ps.executeQuery()
                if(rs.next()) {
                    val data = HashMap<String, String>()
                    data["blogId"] = blogId
                    Log.createBlog(id, date, ip, true, blogId)
                    rs.close()
                    ps.close()
                    reqMaps.getBlogFiles(blogId, id)
                    out.write(json(Shortcut.OK, "you have posted the blog successfully.", data))
                } else {
                    rs.close()
                    ps.close()
                    out.write(json(Shortcut.OTHER, "CREATE BLOG FAILED"))
                    return
                }
            } else {
                Log.createBlog(id, date, ip, false)
                out.write(json(Shortcut.TE, "invalid token"))
                rs.close()
                ps.close()
                return
            }

        } catch (e: SQLException) {
            out.write(json(Shortcut.OTHER, "SQL ERROR"))
            e.printStackTrace()
            return
        }

    }

    private fun getBlog(req: HttpServletRequest?) {
        val map = req!!.parameterMap
        val blogId = map["id"]?.get(0)
        val type = when(map["type"]?.get(0)) {
            "json" -> ShowBlogType.JSON
            else -> ShowBlogType.HTML
        }
        if (blogId.isNullOrEmpty()) {
            out.write(json(Shortcut.AE, "argument mismatch"))
            return
        }

        try {
            val conn = MySQLConn.connection
            val ps = conn.prepareStatement("select * from blog where blog_id = ? limit 1")
            ps.setString(1, blogId)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val data = HashMap<String, String>()
                data["author_id"] = rs.getString("author_id")
                data["nickname"] = User.getNickname(data["author_id"]?:"000000")
                data["title"] = rs.getString("title")
                data["introduction"] = rs.getString("introduction")
                val content = StringUtil.blob2String(rs.getBlob("content")).replace("####blog_id####", blogId)
                data["tag"] = rs.getString("tag")
                data["comment"] = StringUtil.blob2String(rs.getBlob("comment"))
                data["likes"] = StringUtil.blob2String(rs.getBlob("likes"))
                data["last_edit_time"] = StringUtil.getTime(rs.getTimestamp("last_edit_time"))
                data["data"] = StringUtil.blob2String(rs.getBlob("data"))

                when (type) {
                    ShowBlogType.JSON -> {
                        data["content"] = Markdown2Html.parse(content)
                        out.write(json(Shortcut.OK, "return blog successfully", data))
                    }

                    ShowBlogType.HTML -> {
                        data["content"] = Markdown2Html.parse(content)
                        var html = StringUtil.htmlTemplate()
                        val style = StringUtil.markdownAirCss()
                        html = html.replace("####title-author####", "${data["title"]}-${data["nickname"]}")
                            .replace("####style####", style)
                            .replace("####title####", "${data["title"]}")
                            .replace("####nickname####", "${data["nickname"]}")
                            .replace("####last_edit_time####", "${data["last_edit_time"]}")
                            .replace("####introduction####", "${data["introduction"]}")
                            .replace("####content####", "${data["content"]}")

                        out.write(html)
                    }
                }

                rs.close()
                ps.close()
            } else {
                rs.close()
                ps.close()
                out.write(json(Shortcut.BNE, "Blog $blogId does not exist"))
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            out.write(json(Shortcut.OTHER, "SQL ERROR"))
        }

    }

    private fun getBlogPic(req: HttpServletRequest?, resp: HttpServletResponse?) {
        val map = req!!.parameterMap
        val blogId = map["id"]?.get(0)
        val picKey = map["key"]?.get(0)
        if (blogId.isNullOrEmpty() || picKey.isNullOrEmpty()) {
            out.write(APIPublicUser.json(Shortcut.AE, "argument mismatch."))
            return
        }

        val conn = MySQLConn.connection
        try {
            val ps = conn.prepareStatement("select data from blog where blog_id = ? and status = 'normal' limit 1")
            ps.setString(1, blogId)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val jsonFile = File(this.servletContext.getRealPath("/conf/dir"))
                val conf = FileReadUtil.readJson(jsonFile)
                val path = conf.getString("root") +"\\${conf.getString("blog_pics")}\\$blogId\\$picKey"
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
                out.write(APIPublicUser.json(Shortcut.BNE, "the blog $blogId does not found."))
                rs.close()
                ps.close()
            }


        } catch (e: SQLException) {
            e.printStackTrace()
            out.write(APIPublicUser.json(Shortcut.OTHER, "SQL ERROR"))
        } catch (e: Exception) {
            e.printStackTrace()
            out.write(APIPublicUser.json(Shortcut.OTHER, "UNKNOWN EXCEPTION"))
        }
    }

    private fun getBlogList(req: HttpServletRequest?) {
        val map = req!!.parameterMap
        // type, count, date, from, to, tag
        val type = when(map["type"]?.get(0)) {
            "id" -> GetBlogByType.Id
            "time" -> GetBlogByType.Time
            else -> GetBlogByType.Default
        }
        val tag: String = map["tag"]?.get(0)?:""
        val count = map["count"]?.get(0)?.toInt()?:0
        val date = StringUtil.getTime(map["date"]?.get(0))
        val from = map["from"]?.get(0)
        val to = map["to"]?.get(0)


        if((type == GetBlogByType.Id && ((from.isNullOrEmpty() && to.isNullOrEmpty()) || count <= 0)) || (type == GetBlogByType.Time && (date == null || count <= 0)) || (type == GetBlogByType.Default)) {
            out.write(json(Shortcut.AE, "argument mismatch."))
            return
        }

        try {
            val conn = MySQLConn.connection
            when (type) {
                GetBlogByType.Time -> {
                    val blogList = ArrayList<Blog.Outline>()
                    var index = 0
                    val ps = conn.prepareStatement("select blog_id, author_id, title, introduction, tag, last_active_time from blog where last_active_time <= ? and status = 'normal' and tag like ? order by last_active_time desc limit ?")
                    ps.setTimestamp(1, Timestamp(date!!.time))
                    ps.setString(2, "%$tag%")
                    ps.setInt(3, count)
                    val rs = ps.executeQuery()
                    while (rs.next()) {
                        val blog = Blog.Outline(
                            rs.getString("blog_id"),
                            rs.getString("author_id"),
                            rs.getString("title"),
                            rs.getString("introduction"),
                            rs.getString("tag"),
                            rs.getTimestamp("last_active_time")
                        )
                        blog.index = index
                        index++

                        blogList.add(blog)

                    }
                    rs.close()
                    ps.close()

                    val json = jsonBlogOutline(Shortcut.OK, "return blog list successfully.", blogList)
                    out.write(json)

                    return
                }

                GetBlogByType.Id -> {

                    val blogList = ArrayList<Blog.Outline>()
                    var index = 0

                    when {
                        from != null -> {

                            val ps1 = conn.prepareStatement("select last_active_time from blog where blog_id = ?")
                            ps1.setString(1, from)
                            val rs1 = ps1.executeQuery()
                            if (rs1.next()) {
                                val timestamp = rs1.getTimestamp("last_active_time")
                                rs1.close()
                                ps1.close()
                                val ps = conn.prepareStatement("select blog_id, author_id, title, introduction, tag, last_active_time from blog where last_active_time > ? and status = 'normal' and tag like ? order by last_active_time limit ?")
                                ps.setTimestamp(1, timestamp)
                                ps.setString(2, "%$tag%")
                                ps.setInt(3, count)
                                val rs = ps.executeQuery()
                                while (rs.next()) {
                                    val blog = Blog.Outline(
                                        rs.getString("blog_id"),
                                        rs.getString("author_id"),
                                        rs.getString("title"),
                                        rs.getString("introduction"),
                                        rs.getString("tag"),
                                        rs.getTimestamp("last_active_time")
                                    )
                                    blog.index = index
                                    index++

                                    blogList.add(blog)

                                }
                                rs.close()
                                ps.close()
                                val json = jsonBlogOutline(Shortcut.OK, "return blog list successfully.", blogList)
                                out.write(json)
                                return

                            } else {
                                rs1.close()
                                ps1.close()
                                out.write(json(Shortcut.BNE, "blog $from not found."))
                                return
                            }
                        }
                        to != null -> {
                            val ps1 = conn.prepareStatement("select last_active_time from blog where blog_id = ?")
                            ps1.setString(1, to)
                            val rs1 = ps1.executeQuery()
                            if (rs1.next()) {
                                val timestamp = rs1.getTimestamp("last_active_time")
                                rs1.close()
                                ps1.close()
                                val ps = conn.prepareStatement("select blog_id, author_id, title, introduction, tag, last_active_time from blog where last_active_time < ? and status = 'normal' and tag like ? order by last_active_time desc limit ?")
                                ps.setTimestamp(1, timestamp)
                                ps.setString(2, "%$tag%")
                                ps.setInt(3, count)
                                val rs = ps.executeQuery()
                                while (rs.next()) {
                                    val blog = Blog.Outline(
                                        rs.getString("blog_id"),
                                        rs.getString("author_id"),
                                        rs.getString("title"),
                                        rs.getString("introduction"),
                                        rs.getString("tag"),
                                        rs.getTimestamp("last_active_time")
                                    )
                                    blog.index = index
                                    index++

                                    blogList.add(blog)

                                }
                                rs.close()
                                ps.close()
                                val json = jsonBlogOutline(Shortcut.OK, "return blog list successfully.", blogList)
                                out.write(json)
                                return

                            } else {
                                rs1.close()
                                ps1.close()
                                out.write(json(Shortcut.BNE, "blog $from not found."))
                                return
                            }
                        }
                        else -> return
                    }

                }

                GetBlogByType.Default -> return
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            out.write(json(Shortcut.OTHER, "SQL ERROR"))
        }


    }


    private fun test() {
        val jsonFile = File(this.servletContext.getRealPath("/conf/dir"))
        val conf = FileReadUtil.readJson(jsonFile)
        out.write(conf.toJSONString() ?:StringUtil.json(Shortcut.OTHER, "Failed"))
    }

    companion object {

        fun json(shortcut: Shortcut, msg: String, data: HashMap<String, String>? = null): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            if(data != null){
                map["data"] = JSONObject(data as Map<String, Any>?)
            }
            return map.toJSONString()
        }

        fun jsonBlogOutline(shortcut: Shortcut, msg: String, data: ArrayList<Blog.Outline>? = null): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            if(data != null){
                map["data"] = JSONArray(data as List<Any>?)
            }
            return map.toJSONString()
        }
    }

    enum class GetBlogByType {
        Time, Id, Default
    }

    enum class ShowBlogType {
        JSON, HTML
    }

}