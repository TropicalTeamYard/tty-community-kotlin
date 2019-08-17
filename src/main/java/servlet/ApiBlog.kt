package servlet

import enums.Shortcut
import model.Blog
import model.Blog.*
import model.Message
import model.User
import util.CONF
import util.Value
import util.Value.json
import util.Value.fields
import util.Value.value
import util.conn.MySQLConn
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
class ApiBlog : HttpServlet() {
    private var ip: String = "0.0.0.0"
    private lateinit var out: PrintWriter

    private fun <T> Message<T>.write() {
        out.write(this.json())
    }

    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp?.characterEncoding = "UTF-8"
        req?.characterEncoding = "UTF-8"
        out = resp!!.writer
        ip = Value.getIP(req!!)
        val route = try {
            req.requestURI.substring(20)
        } catch (e: StringIndexOutOfBoundsException) {
            Message<Any>(Shortcut.AE, "invalid request").write()
            return
        }

        when (route) {
            "test" -> {
                // http://localhost:8080/community/api/blog/test
                test()
            }

            "create" -> {
                // http://localhost:8080/community/api/blog/create
                Blog.create(req).write()
            }

            "get" -> {
                // http://localhost:8080/community/api/blog/get?id=746235507&type=json
                getBlog(req)
            }


            "list" -> {
                // http://localhost:8080/community/api/blog/list?type=id&to=1293637237&count=8 # id 为 `to` 之前日期的 count 条记录 &tag=?
                // http://localhost:8080/community/api/blog/list?type=time&date=2019/8/25-03:24:52&count=2 # date 及之前日期的 count 条记录 &tag=?
                getBlogList(req)
            }

            "picture" -> {
                // http://localhost:8080/community/api/blog/picture?id=700642438&index=0
                getBlogPic(req, resp)
            }

            else -> {
                out.write(json(Shortcut.AE, "invalid request"))
            }
        }
    }

    private fun getBlog(req: HttpServletRequest) {
        val fields = req.parameterMap.fields()
        val blogId = fields["id"]
        if (blogId.isNullOrEmpty()) {
            Message<Any>(Shortcut.AE, "argument mismatch").write()
        } else {
            val message = Blog.getDetailById(blogId)
            when (BlogContentType.getType(fields["type"])) {
                BlogContentType.JSON -> {
                    message.data?.parse()
                    message.write()
                }
                BlogContentType.HTML -> {
                    val html = message.data?.parseHtml()
                    if (html != null) {
                        out.write(html)
                    } else {
                        //todo write the 404 page
                        Message<Any>(Shortcut.BNE, "blog $blogId not found").write()
                    }
                }
            }
        }
    }

    private fun getBlogPic(req: HttpServletRequest?, resp: HttpServletResponse?) {
        val map = req!!.parameterMap
        val blogId = map["id"]?.get(0)
        val picKey = map["key"]?.get(0)
        if (blogId.isNullOrEmpty() || picKey.isNullOrEmpty()) {
            out.write(json(Shortcut.AE, "argument mismatch."))
            return
        }

        val status = BlogStatus.NORMAL

        val conn = MySQLConn.connection
        try {
            val ps = conn.prepareStatement("select data from blog where blog_id = ? and status <= ? limit 1")
            ps.setString(1, blogId)
            ps.setInt(2, status.value())
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
                out.write(json(Shortcut.BNE, "blog $blogId not found."))
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

    private fun getBlogList(req: HttpServletRequest) {
        val fields = req.parameterMap.fields()
        val tag = fields["tag"] ?: ""
        val date = Value.getTime(fields["date"])
        val id = fields["id"]
        val count = fields["count"].value(5)

        when (BlogListType.getType(fields["type"])) {
            BlogListType.ID -> {
                if (id.isNullOrEmpty()) {
                    Message<Any>(Shortcut.AE, "id can't be null").write()
                } else {
                    Blog.getBlogListById(id, tag, count).write()
                }
            }
            BlogListType.TIME -> {
                if (date == null) {
                    Message<Any>(Shortcut.AE, "date can't be null").write()
                } else {
                    Blog.getBlogListByTime(date, tag, count).write()
                }
            }
            else -> {
                Message<Any>(Shortcut.AE, "type not allowed").write()
            }
        }

    }

    private fun getBlogs(ps: PreparedStatement): ArrayList<Outline> {
        val list = ArrayList<Outline>()
        val rs = ps.executeQuery()
        while (rs.next()) {
            val blogId = rs.getString("blog_id")
            val type = rs.getInt("type")
            val author = rs.getString("author_id")
            val nickname = User.getNicknameById(author) ?: Value.DEFAULT_NICKNAME
            val title = rs.getString("title")
            val introduction = rs.getString("introduction")
            val tag = rs.getString("tag")
            val lastActiveTime = rs.getTimestamp("last_active_time")
            val blog = Outline(blogId, type, author, title, introduction, tag, lastActiveTime, nickname, 0)

            list.add(blog)

        }
        rs.close()
        ps.close()
        return list
    }

    private fun test() {
        out.write(CONF.root + "\n")
        out.write(CONF.conf.server)
    }



}