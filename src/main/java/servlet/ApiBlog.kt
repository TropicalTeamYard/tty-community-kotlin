package servlet

import enums.Shortcut
import model.Blog
import model.Blog.*
import model.Message
import util.CONF
import util.Value
import util.Value.fields
import util.Value.value
import util.conn.MySQLConn
import util.file.FileUtil
import java.io.PrintWriter
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "api_blog", urlPatterns = ["/api/blog/*"])
class ApiBlog : HttpServlet() {
    private var ip: String = "0.0.0.0"
    private lateinit var out: PrintWriter

    private fun <T> Message<T>.write() { out.write(this.json()) }

    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) { doPost(req, resp) }
    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp!!.characterEncoding = "UTF-8"
        req!!.characterEncoding = "UTF-8"
        out = resp.writer
        ip = Value.getIP(req)
        val route = try {
            req.requestURI.substring(20)
        } catch (e: StringIndexOutOfBoundsException) {
            Message<Any>(Shortcut.AE, "invalid request").write()
            return
        }

        when (route) {
            // http://localhost:8080/community/api/blog/create
            "create" -> Blog.create(req).write()

            // http://localhost:8080/community/api/blog/get?id=746235507&type=json
            "get" -> getBlog(req)

            // http://localhost:8080/community/api/blog/list?type=id&to=1293637237&count=8&tag=000000 # id 为 `to` 之前日期的 count 条记录 &tag=?
            // http://localhost:8080/community/api/blog/list?type=time&time=2019/8/25-03:24:52&count=2&tag=000000 # date 及之前日期的 count 条记录 &tag=?
            "list" -> getBlogList(req)

            // http://localhost:8080/community/api/blog/picture?id=700642438&key=0
            "picture" -> getBlogPic(req, resp, 0.4, 0.6)

            // http://localhost:8080/community/api/blog/picture_raw?id=700642438&key=0
            "picture/raw" -> getBlogPic(req, resp, 1.0, 1.0)

            // http://localhost:8080/community/api/blog/test
            "test" -> test()

            else -> Message<Any>(Shortcut.AE, "invalid request").write()
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

    private fun getBlogPic(req: HttpServletRequest, resp: HttpServletResponse, scale: Double, quality: Double) {
        val map = req.parameterMap.fields()
        val blogId = map["id"]
        val picKey = map["key"]
        if (blogId.isNullOrEmpty() || picKey.isNullOrEmpty()) {
            Message<Any>(Shortcut.AE, "argument mismatch").write()
        } else {
            val status = BlogStatus.NORMAL
            val conn = MySQLConn.connection
            try {
                val ps = conn.prepareStatement("select data from blog where blog_id = ? and status <= ? limit 1")
                ps.setString(1, blogId)
                ps.setInt(2, status.value())
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val path = CONF.conf.blog + "/$blogId/$picKey"
                    FileUtil.writePicture2Response(resp, path, scale, quality)
                } else {
                    Message<Any>(Shortcut.BNE, "blog $blogId not found").write()
                }

                rs.close()
                ps.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Message<Any>(Shortcut.OTHER, "unknown error").write()
            }
        }
    }

    private fun getBlogList(req: HttpServletRequest) {
        val fields = req.parameterMap.fields()
        val tag = fields["tag"] ?: ""
        val time = Value.getTime(fields["time"])
        val id = fields["id"]
        val count = fields["count"].value(5)

        when (BlogListType.parse(fields["type"])) {
            BlogListType.ID -> {
                if (id.isNullOrEmpty()) {
                    Message<Any>(Shortcut.AE, "id can't be null").write()
                } else {
                    Blog.getBlogListById(id, tag, count).write()
                }
            }
            BlogListType.TIME -> {
                if (time == null) {
                    Message<Any>(Shortcut.AE, "time can't be null").write()
                } else {
                    Blog.getBlogListByTime(time, tag, count).write()
                }
            }
            else -> {
                Message<Any>(Shortcut.AE, "type not allowed").write()
            }
        }

    }

    private fun test() {
        out.write(CONF.root + "\n")
        out.write(CONF.conf.server)
    }

}