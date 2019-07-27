package servlet

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import model.*
import util.*
import java.io.File
import java.io.PrintWriter
import java.nio.charset.Charset
import java.sql.SQLException
import java.sql.*
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.collections.HashMap


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
                // http://localhost:8080/community/api/user?method=change_detail_info&id=1285609993&token=E0DC9F89E9C06F36072C27138833230B&params=personal_signature::helloworld&params=key::value
                val map = req.parameterMap
                val id = map["id"]?.get(0)
                val token = map["token"]?.get(0)

                if(reqIP.isEmpty() || reqIP == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty()){
                    out.write(StringUtil.json(Shortcut.AE, "argument mismatch."))
                    return
                }

                val changeDetailInfo = ChangeDetailInfo(id, token, reqIP)

                val params = map["fields"]?: arrayOf()
                for(item in params) {
                    try {
                        val key = item.split("::")[0]
                        val value = item.split("::")[1]
                        changeDetailInfo.changedItem[key] = value
                    } catch (e: IndexOutOfBoundsException) {
                        continue
                    }
                }
                if (changeDetailInfo.changedItem.size > 0) {
                    out.write(changeDetailInfo.submit())
                } else {
                    out.write(StringUtil.json(Shortcut.OTHER, "Nothing changed"))
                }

                ReqType.ChangeDetailInfo
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

            "test" -> {
                // http://localhost:8080/community/api/user?method=test
                val jsonFile = File(this.servletContext.getRealPath("/conf/dir"))
                val conf = StringUtil.jsonFromFile(jsonFile)
                out.write(conf?.toJSONString()?:StringUtil.json(Shortcut.OTHER, "Failed"))
                ReqType.Test
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

@WebServlet(name = "api_public_user", urlPatterns = ["/api/public/user/*"])
class APIPublicUser: HttpServlet() {
    private var ip: String = "0.0.0.0"
    lateinit var out: PrintWriter

    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?){
        val reqIP = APIUser.getIPAddr(req!!)?:"0.0.0.0"
        resp?.writer?.write("API: APIPublicUser\nIP: $reqIP\n")
        doPost(req, resp)
    }


    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp?.characterEncoding = "utf-8"
        req?.characterEncoding = "utf-8"
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
        val conf = StringUtil.jsonFromFile(jsonFile)
        out.write(conf?.toJSONString()?:StringUtil.json(Shortcut.OTHER, "Failed"))
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

            ps = conn.prepareStatement("select * from user where id = ? limit 1")
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

        fun getNickname(id: String): String? {
            val conn = MySQLConn.connection
            try {

                val ps = conn.prepareStatement("select nickname from user where id = ? limit 1")
                ps.setString(1, id)
                val rs = ps.executeQuery()
                val nickname =
                if (rs.next()) {
                    rs.getString("nickname")
                } else {
                    null
                }
                rs.close()
                ps.close()
                return nickname
            } catch (e: SQLException) {
                e.printStackTrace()
                return null
            }
        }
    }

}


@WebServlet(name = "api_blog", urlPatterns = ["/api/blog/*"])
class APIBlog: HttpServlet() {
    private var ip: String = "0.0.0.0"
    private lateinit var out: PrintWriter
    private val date = java.util.Date()
    private val time = Timestamp(date.time)

    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?){
        val reqIP = APIUser.getIPAddr(req!!)?:"0.0.0.0"
        resp?.writer?.write("API: APIBlog\nIP: $reqIP\n")
        doPost(req, resp)
    }


    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp?.characterEncoding = "utf-8"
        req?.characterEncoding = "utf-8"
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
                test()
                return
            }

            "create" -> {
                create(req)
                return
            }

            "get" -> {
                // http://localhost:8080/community/api/blog/get?blog_id=1293756613
                getBlog(req)
                return
            }


            "list" -> {
                // http://localhost:8080/community/api/blog/list?type=id&to=1293637237&count=8 # id 为 `to` 之前日期的 count 条记录
                // http://localhost:8080/community/api/blog/list?type=id&from=1293637237&count=8 # id 为 `from` 之后日期的 count 条记录
                // http://localhost:8080/community/api/blog/list?type=time&date=2019/7/26-03:24:52&count=5 # date 及之前日期的 count 条记录
                getBlogList(req)
                return
            }

            else -> {
                out.write(json(Shortcut.AE, "invalid request"))
                return
            }
        }
    }

    private fun create(req: HttpServletRequest?) {
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
            if (rs.next() && token == StringUtil.getMd5(rs.getString("token"))) {
                rs.close()
                ps.close()
                ps = conn.prepareStatement("insert into blog (blog_id, author_id, title, introduction, content, tag, last_edit_time, status, data, log, comment, likes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                ps.setString(1, blogId)
                ps.setString(2, id)
                ps.setString(3, title)
                ps.setString(4, introduction)
                ps.setString(5, content)
                ps.setString(6, tag)
                ps.setTimestamp(7, time)
                ps.setString(8, "normal")
                ps.setString(9, "files:$filesCount")
                ps.setString(10, "init\n")
                ps.setString(11, "")
                ps.setString(12, "")
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
                    reqMaps.getBlogFiles(blogId, id)
                    out.write(json(Shortcut.OK, "you have posted the blog successfully.", data))

                } else {
                    rs.close()
                    out.write(json(Shortcut.AE, "CREATE BLOG FAILED"))
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
        val blogId = req?.getParameter("blog_id")
        if (blogId.isNullOrEmpty()) {
            out.write(json(Shortcut.AE, "argument mismatch."))
            return
        }

        try {
            val conn = MySQLConn.connection
            val ps = conn.prepareStatement("select * from blog where blog_id = ? and status = 'normal' limit 1")
            ps.setString(1, blogId)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val blog = Blog.Detail(
                    blogId,
                    rs.getString("author_id"),
                    rs.getString("title"),
                    rs.getString("introduction"),
                    rs.getString("tag"),
                    rs.getTimestamp("last_edit_time")
                )

                blog.authorNickname = APIPublicUser.getNickname(blog.author).toString()
                blog.content = rs.getString("content")
                blog.comment = rs.getString("comment")
                blog.likes = rs.getString("likes")
                blog.status = rs.getString("status")
                rs.close()
                ps.close()
                out.write(jsonBlogDetail(Shortcut.OK, "return blog successfully.", blog))
                return
            } else {
                rs.close()
                ps.close()
                out.write(json(Shortcut.BNE, "blog not found"))
                return
            }

        } catch (e: SQLException) {
            e.printStackTrace()
            out.write(json(Shortcut.OTHER, "SQL ERROR"))
        }
    }

    private fun getBlogList(req: HttpServletRequest?) {
        val map = req!!.parameterMap
        //val paramSet = setOf("type", "count", "date", "from", "to")
        val type = when(map["type"]?.get(0)) {
            "id" -> GetBlogType.Id
            "time" -> GetBlogType.Time
            else -> GetBlogType.Default
        }
        val count = map["count"]?.get(0)?.toInt()?:0
        val date = StringUtil.getTime(map["date"]?.get(0))
        val from = map["from"]?.get(0)
        val to = map["to"]?.get(0)


        if((type == GetBlogType.Id && ((from.isNullOrEmpty() && to.isNullOrEmpty()) || count <= 0)) || (type == GetBlogType.Time && (date == null || count <= 0)) || (type == GetBlogType.Default)) {
            out.write(json(Shortcut.AE, "argument mismatch."))
            return
        }

        try {
            val conn = MySQLConn.connection
            when (type) {
                GetBlogType.Time -> {
                    val blogList = ArrayList<Blog.Outline>()
                    var index = 0
                    val ps = conn.prepareStatement("select blog_id, author_id, title, introduction, tag, last_edit_time from blog where last_edit_time <= ? and status = 'normal' order by last_edit_time desc limit ?")
                    ps.setTimestamp(1, Timestamp(date!!.time))
                    ps.setInt(2, count)
                    val rs = ps.executeQuery()
                    while (rs.next()) {
                        val blog = Blog.Outline(
                            rs.getString("blog_id"),
                            rs.getString("author_id"),
                            rs.getString("title"),
                            rs.getString("introduction"),
                            rs.getString("tag"),
                            rs.getTimestamp("last_edit_time")
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

                GetBlogType.Id -> {

                    val blogList = ArrayList<Blog.Outline>()
                    var index = 0

                    when {
                        from != null -> {

                            val ps1 = conn.prepareStatement("select last_edit_time from blog where blog_id = ?")
                            ps1.setString(1, from)
                            val rs1 = ps1.executeQuery()
                            if (rs1.next()) {
                                val timestamp = rs1.getTimestamp("last_edit_time")
                                rs1.close()
                                ps1.close()
                                val ps = conn.prepareStatement("select blog_id, author_id, title, introduction, tag, last_edit_time from blog where last_edit_time > ? and status = 'normal' order by last_edit_time limit ?")
                                ps.setTimestamp(1, timestamp)
                                ps.setInt(2, count)
                                val rs = ps.executeQuery()
                                while (rs.next()) {
                                    val blog = Blog.Outline(
                                        rs.getString("blog_id"),
                                        rs.getString("author_id"),
                                        rs.getString("title"),
                                        rs.getString("introduction"),
                                        rs.getString("tag"),
                                        rs.getTimestamp("last_edit_time")
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
                            val ps1 = conn.prepareStatement("select last_edit_time from blog where blog_id = ?")
                            ps1.setString(1, to)
                            val rs1 = ps1.executeQuery()
                            if (rs1.next()) {
                                val timestamp = rs1.getTimestamp("last_edit_time")
                                rs1.close()
                                ps1.close()
                                val ps = conn.prepareStatement("select blog_id, author_id, title, introduction, tag, last_edit_time from blog where last_edit_time < ? and status = 'normal' order by last_edit_time desc limit ?")
                                ps.setTimestamp(1, timestamp)
                                ps.setInt(2, count)
                                val rs = ps.executeQuery()
                                while (rs.next()) {
                                    val blog = Blog.Outline(
                                        rs.getString("blog_id"),
                                        rs.getString("author_id"),
                                        rs.getString("title"),
                                        rs.getString("introduction"),
                                        rs.getString("tag"),
                                        rs.getTimestamp("last_edit_time")
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

                GetBlogType.Default -> return
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            out.write(json(Shortcut.OTHER, "SQL ERROR"))
        }


    }


    private fun test() {
        val jsonFile = File(this.servletContext.getRealPath("/conf/dir"))
        val conf = StringUtil.jsonFromFile(jsonFile)
        out.write(conf?.toJSONString()?:StringUtil.json(Shortcut.OTHER, "Failed"))
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

        private fun jsonBlogDetail(shortcut: Shortcut, msg: String, data: Blog.Detail): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            map["data"] = data
            return map.toJSONString()
        }

        fun jsonBlogOutline(shortcut: Shortcut, msg: String, data: ArrayList<Blog.Outline>): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            map["data"] = JSONArray(data as List<Any>?)
            return map.toJSONString()
        }
    }

    enum class GetBlogType {
        Time, Id, Default
    }

}