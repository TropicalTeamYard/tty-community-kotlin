package servlet

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import model.Blog
import model.Blog.Companion.Type.Companion.parse
import model.Blog.Companion.Type.Companion.value
import model.User
import util.CONF
import util.Value
import util.Value.string
import util.conn.MySQLConn
import util.enums.Shortcut
import util.log.Log
import util.parse.BlogRequestParser
import util.parse.IP
import util.parse.Markdown2Html
import java.io.FileInputStream
import java.io.PrintWriter
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Timestamp
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "api_blog", urlPatterns = ["/api/blog/*"])
class APIBlog : HttpServlet() {
    private var ip: String = "0.0.0.0"
    private lateinit var out: PrintWriter


    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp?.characterEncoding = "UTF-8"
        req?.characterEncoding = "UTF-8"
        out = resp!!.writer
        ip = IP.getIPAddr(req!!)
        val route = try {
            req.requestURI.substring(20)
        } catch (e: StringIndexOutOfBoundsException) {
            out.write(Value.json(Shortcut.AE, "invalid request"))
            return
        }

        when (route) {
            "test" -> {
                // http://localhost:8080/community/api/blog/test
                test()
            }

            "create" -> {
                // http://localhost:8080/community/api/blog/create
                create(req)
            }

            "get" -> {
                // http://localhost:8080/community/api/blog/get?id=746235507&type=json
                getBlog(req)
            }


            "list" -> {
                // http://localhost:8080/community/api/blog/list?type=id&to=1293637237&count=8 # id 为 `to` 之前日期的 count 条记录 &tag=?
                // http://localhost:8080/community/api/blog/list?type=id&from=1293637237&count=8 # id 为 `from` 之后日期的 count 条记录 &tag=?
                // http://localhost:8080/community/api/blog/list?type=time&date=2019/8/25-03:24:52&count=2 # date 及之前日期的 count 条记录 &tag=?
                getBlogList(req)
            }

            "picture" -> {
                // http://localhost:8080/community/api/blog/picture?id=700642438&index=0
                getBlogPic(req, resp)
            }

            else -> {
                out.write(Value.json(Shortcut.AE, "invalid request"))
            }
        }
    }


    private fun create(req: HttpServletRequest?) {
        val date = java.util.Date()
        val time = Timestamp(date.time)
        val params: HashMap<String, String>
        val reqMaps = BlogRequestParser(req!!)
        params = reqMaps.getField()
        val id = params["id"]
        val token = params["token"]
        val type = params["type"].parse
        val title = params["title"]
        val introduction = params["introduction"]
        val content = params["content"]
        val tag = params["tag"]
        val filesCount = params["file_count"]

        if (ip == "0.0.0.0" || id.isNullOrEmpty() || token.isNullOrEmpty() || title.isNullOrEmpty() || introduction.isNullOrEmpty() || content.isNullOrEmpty() || tag.isNullOrEmpty() || filesCount.isNullOrEmpty()) {
            out.write(Value.json(Shortcut.AE, "argument mismatch."))
            return
        }

        val blogId = ("$ip$id$token${date.time}${(1000..9999).random()}".hashCode() and Integer.MAX_VALUE).toString()

        try {
            val conn = MySQLConn.connection
            var ps = conn.prepareStatement("select * from user where id = ? limit 1")
            ps.setString(1, id)
            var rs = ps.executeQuery()
            if (rs.next() && token == Value.getMD5(rs.getString("token"))) {
                rs.close()
                ps.close()
                ps =
                    conn.prepareStatement("insert into blog (blog_id, type, author_id, title, introduction, content, tag, last_edit_time, last_active_time, status, data, log, comment, likes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                ps.setString(1, blogId)
                ps.setInt(2, type.value)
                ps.setString(3, id)
                ps.setString(4, title)
                ps.setString(5, introduction)
                ps.setString(6, content)
                ps.setString(7, tag)
                ps.setTimestamp(8, time) // last_edit_time
                ps.setTimestamp(9, time) // last_active_time
                ps.setString(10, "normal") // status
                ps.setString(11, "") // data
                ps.setString(12, "init\n") // log
                ps.setString(13, "") // comment
                ps.setString(14, "") // likes
                ps.execute()
                ps.close()
                ps = conn.prepareStatement("select * from blog where blog_id = ? limit 1")
                ps.setString(1, blogId)
                rs = ps.executeQuery()
                if (rs.next()) {
                    val data = HashMap<String, String>()
                    data["blogId"] = blogId
                    Log.createBlog(id, date, ip, true, blogId)
                    rs.close()
                    ps.close()
                    reqMaps.getBlogFiles(blogId)
                    out.write(Value.json(Shortcut.OK, "you have posted the blog successfully.", data))
                } else {
                    rs.close()
                    ps.close()
                    out.write(Value.json(Shortcut.OTHER, "CREATE BLOG FAILED"))
                    return
                }
            } else {
                Log.createBlog(id, date, ip, false)
                out.write(Value.json(Shortcut.TE, "invalid token"))
                rs.close()
                ps.close()
                return
            }

        } catch (e: SQLException) {
            out.write(Value.json(Shortcut.OTHER, "SQL ERROR"))
            e.printStackTrace()
            return
        }

    }

    private fun getBlog(req: HttpServletRequest?) {
        val map = req!!.parameterMap
        val blogId = map["id"]?.get(0)
        val type = when (map["type"]?.get(0)) {
            "json" -> ShowBlogType.JSON
            else -> ShowBlogType.HTML
        }
        if (blogId.isNullOrEmpty()) {
            out.write(Value.json(Shortcut.AE, "argument mismatch"))
            return
        }

        try {
            val conn = MySQLConn.connection
            val ps = conn.prepareStatement("select * from blog where blog_id = ? and status = 'normal' limit 1")
            ps.setString(1, blogId)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val data = HashMap<String, String>()
                data["author_id"] = rs.getString("author_id")
                data["nickname"] = User.getNickname(data["author_id"] ?: "000000")
                data["title"] = rs.getString("title")
                data["introduction"] = rs.getString("introduction").replace("####blog_id####", blogId)
                val content = rs.getBlob("content").string().replace("####blog_id####", blogId)
                data["tag"] = rs.getString("tag")
                data["comment"] = rs.getBlob("comment").string()
                data["likes"] = rs.getBlob("likes").string()
                data["last_edit_time"] = Value.getTime(rs.getTimestamp("last_edit_time"))
                data["last_active_time"] = Value.getTime(rs.getTimestamp("last_active_time"))
                data["data"] = rs.getBlob("data").string()

                when (type) {
                    ShowBlogType.JSON -> {
                        data["content"] = content
                        out.write(Value.json(Shortcut.OK, "get blog successfully", data))
                    }

                    ShowBlogType.HTML -> {
                        data["content"] = Markdown2Html.parse(content)
                        var html = Value.htmlTemplate()
                        val style = Value.markdownAirCss()
                        html = html.replace("####title-author####", "${data["title"]}-${data["nickname"]}")
                            .replace("####style####", style)
                            .replace("####title####", "${data["title"]}")
                            .replace("####nickname####", "${data["nickname"]}")
                            .replace("####last_edit_time####", "${data["last_edit_time"]}")
                            .replace("####last_active_time####", "${data["last_active_time"]}")
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
                out.write(Value.json(Shortcut.BNE, "Blog $blogId does not exist"))
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            out.write(Value.json(Shortcut.OTHER, "SQL ERROR"))
        }

    }

    private fun getBlogPic(req: HttpServletRequest?, resp: HttpServletResponse?) {
        val map = req!!.parameterMap
        val blogId = map["id"]?.get(0)
        val picKey = map["key"]?.get(0)
        if (blogId.isNullOrEmpty() || picKey.isNullOrEmpty()) {
            out.write(Value.json(Shortcut.AE, "argument mismatch."))
            return
        }

        val conn = MySQLConn.connection
        try {
            val ps = conn.prepareStatement("select data from blog where blog_id = ? and status = 'normal' limit 1")
            ps.setString(1, blogId)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val path = CONF.conf.blog + "/$blogId/$picKey"
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
                out.write(Value.json(Shortcut.BNE, "the blog $blogId does not found."))
                rs.close()
                ps.close()
            }


        } catch (e: SQLException) {
            e.printStackTrace()
            out.write(Value.json(Shortcut.OTHER, "SQL ERROR"))
        } catch (e: Exception) {
            e.printStackTrace()
            out.write(Value.json(Shortcut.OTHER, "UNKNOWN EXCEPTION"))
        }
    }

    private fun getBlogList(req: HttpServletRequest?) {
        val map = req!!.parameterMap
        // type, count, date, from, to, tag
        val type = when (map["type"]?.get(0)) {
            "id" -> GetBlogByType.Id
            "time" -> GetBlogByType.Time
            else -> GetBlogByType.Default
        }
        val tag: String = map["tag"]?.get(0) ?: ""
        val count = map["count"]?.get(0)?.toInt() ?: 0
        val date = Value.getTime(map["date"]?.get(0))
        val from = map["from"]?.get(0)
        val to = map["to"]?.get(0)


        if ((type == GetBlogByType.Id && ((from.isNullOrEmpty() && to.isNullOrEmpty()) || count <= 0)) || (type == GetBlogByType.Time && (date == null || count <= 0)) || (type == GetBlogByType.Default)) {
            out.write(Value.json(Shortcut.AE, "argument mismatch."))
            return
        }

        try {
            val conn = MySQLConn.connection
            when (type) {
                GetBlogByType.Time -> {

                    val ps =
                        conn.prepareStatement("select blog_id, author_id, type, title, introduction, tag, last_active_time from blog where last_active_time <= ? and status = 'normal' and tag like ? order by last_active_time desc limit ?")
                    ps.setTimestamp(1, Timestamp(date!!.time))
                    ps.setString(2, "%$tag%")
                    ps.setInt(3, count)

                    val blogList = getBlogs(ps)

                    val json = jsonBlogOutline(Shortcut.OK, "get blog list successfully.", blogList)
                    out.write(json)

                    return
                }

                GetBlogByType.Id -> {

                    when {
                        from != null -> {
                            val ps1 = conn.prepareStatement("select last_active_time from blog where blog_id = ?")
                            ps1.setString(1, from)
                            val rs1 = ps1.executeQuery()
                            if (rs1.next()) {
                                val timestamp = rs1.getTimestamp("last_active_time")
                                rs1.close()
                                ps1.close()
                                val ps =
                                    conn.prepareStatement("select blog_id, author_id, type, title, introduction, tag, last_active_time from blog where last_active_time > ? and status = 'normal' and tag like ? order by last_active_time limit ?")
                                ps.setTimestamp(1, timestamp)
                                ps.setString(2, "%$tag%")
                                ps.setInt(3, count)
                                val blogList = getBlogs(ps)
                                val json = jsonBlogOutline(Shortcut.OK, "get blog list successfully.", blogList)
                                out.write(json)
                                return
                            } else {
                                rs1.close()
                                ps1.close()
                                out.write(Value.json(Shortcut.BNE, "blog $from not found."))
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
                                val ps =
                                    conn.prepareStatement("select blog_id, author_id, type, title, introduction, tag, last_active_time from blog where last_active_time < ? and status = 'normal' and tag like ? order by last_active_time desc limit ?")
                                ps.setTimestamp(1, timestamp)
                                ps.setString(2, "%$tag%")
                                ps.setInt(3, count)
                                val blogList = getBlogs(ps)
                                val json = jsonBlogOutline(Shortcut.OK, "get blog list successfully.", blogList)
                                out.write(json)
                                return

                            } else {
                                rs1.close()
                                ps1.close()
                                out.write(Value.json(Shortcut.BNE, "blog $from not found."))
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
            out.write(Value.json(Shortcut.OTHER, "SQL ERROR"))
        }

    }

    private fun getBlogs(ps: PreparedStatement): ArrayList<Blog.Outline> {
        val blogList = ArrayList<Blog.Outline>()
        var index = 0
        val rs = ps.executeQuery()
        while (rs.next()) {
            val blogId = rs.getString("blog_id")
            val type = rs.getInt("type")
            val author = rs.getString("author_id")
            val nickname = User.getNickname(author)
            val title = rs.getString("title").replace("####nickname####", nickname)
            val introduction = rs.getString("introduction").replace("####blog_id####", blogId)
            val tag = rs.getString("tag")
            val lastActiveTime = rs.getTimestamp("last_active_time")
            val blog = Blog.Outline(
                blogId,
                type.toString(),
                author,
                title,
                introduction,
                tag,
                lastActiveTime,
                nickname
            )
            blog.index = index
            index++

            blogList.add(blog)

        }
        rs.close()
        ps.close()
        return blogList
    }

    private fun test() {
        out.write(CONF.root + "\n")
        out.write(CONF.conf.server)
    }


    companion object {
        fun jsonBlogOutline(shortcut: Shortcut, msg: String, data: ArrayList<Blog.Outline>? = null): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            if (data != null) {
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