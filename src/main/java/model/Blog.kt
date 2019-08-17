package model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import enums.Shortcut
import model.Blog.BlogType.Companion.parse
import model.log.Log
import util.CONF
import util.Value
import util.Value.string
import util.conn.MySQLConn
import util.parse.Markdown2Html
import util.parse.MultipleForm
import util.parse.Time.getFormattedTime
import java.sql.Timestamp
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.collections.ArrayList

interface Blog {
    val blogId: String
    val type: Int
    val author: String
    val nickname: String
    var title: String
    var introduction: String
    val tag: String
    val lastActiveTime: Date
    val status: Int

    open class Outline(
        override val blogId: String,
        override val type: Int,
        override val author: String,
        override var title: String,
        override var introduction: String,
        override val tag: String,
        override val lastActiveTime: Date,
        override val nickname: String,
        override val status: Int
    ) : Blog {
        open fun parse() = apply {
            introduction = introduction.replace("####blog_id####", blogId)
            title = title.replace("####nickname####", nickname)
        }
    }

    class Detail(
        override val blogId: String,
        override val type: Int,
        override val author: String,
        override var title: String,
        override var introduction: String,
        override val tag: String,
        override val lastActiveTime: Date,
        override val nickname: String,
        override var status: Int,
        private var content: String,
        comments: String,
        likes: String,
        var lastEditTime: Date
    ) : Outline(blogId, type, author, title, introduction, tag, lastActiveTime, nickname, status) {
        val comments: ArrayList<Comment> = gson.fromJson(comments, object : TypeToken<ArrayList<Comment>>() {}.type)
        val likes: ArrayList<Like> = gson.fromJson(likes, object : TypeToken<ArrayList<Like>>() {}.type)

        override fun parse() = apply {
            // todo
            introduction = introduction.replace("####blog_id####", blogId)
            title = title.replace("####nickname####", nickname)
            content = content.replace("####blog_id####", blogId)
        }

        fun parseHtml(): String {
            content = Markdown2Html.parse(content)
            val html = Value.htmlTemplate()
            val style = Value.markdownAirCss()

            return html
                .replace("####nickname####", nickname)
                .replace("####title-author####", "$title-$nickname")
                .replace("####style####", style)
                .replace("####title####", title)
                .replace("####last_edit_time####", lastEditTime.getFormattedTime())
                .replace("####last_active_time####", lastActiveTime.getFormattedTime())
                .replace("####introduction####", introduction)
                .replace("####content####", content)
        }
    }

    data class Comment(val id: String, val nickname: String, val time: String)
    data class Like(val id: String, val nickname: String)

    companion object {
        val gson = Gson()

        // checked
        private fun create(type: BlogType, author: String, title: String, introduction: String, content: String, tag: String, time: Timestamp, status: BlogStatus = BlogStatus.NORMAL): Message<Outline> {
            try {
                val conn = MySQLConn.connection
                val blogId =
                    ("$author$content${time.time}$tag${(1000..9999).random()}".hashCode() and Integer.MAX_VALUE).toString()
                val ps =
                    conn.prepareStatement("insert into blog (blog_id, type, author_id, title, introduction, content, tag, last_edit_time, last_active_time, status, data, log, comments, likes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                ps.setString(1, blogId)
                ps.setInt(2, type.value())
                ps.setString(3, author)
                ps.setString(4, title)
                ps.setString(5, introduction)
                ps.setString(6, content)
                ps.setString(7, tag) // id of topic
                ps.setTimestamp(8, time) // last_edit_time
                ps.setTimestamp(9, time) // last_active_time
                ps.setInt(10, status.value()) // status
                ps.setString(11, "") // data
                ps.setString(12, "init\n") // log
                ps.setString(13, "[]") // comments
                ps.setString(14, "[]") // likes
                ps.execute()
                ps.close()
                getOutlineById(blogId, BlogStatus.ALL)?.let { return Message(Shortcut.OK, "blog created, id $blogId", it.parse()) }
                return Message(Shortcut.OTHER, "fail to create blog")
            } catch (e: Exception) {
                e.printStackTrace()
                return Message(Shortcut.OTHER, "unknown error")
            }
        }

        // checked
        fun create(req: HttpServletRequest): Message<Outline> {
            val multipleForm = MultipleForm(req).build()
            val ip = Value.getIP(req)

            return try {
                val fields = multipleForm.fields

                val author = fields["author"]
                val token = fields["token"]
                val type = fields["type"].parse()
                val title = fields["title"]
                val introduction = fields["introduction"]
                val content = fields["content"]
                val tag = fields["tag"]
                val timestamp = Timestamp(Date().time)

                if (ip == "0.0.0.0" || author.isNullOrEmpty() || token.isNullOrEmpty() || title.isNullOrEmpty() || introduction.isNullOrEmpty() || content.isNullOrEmpty() || tag.isNullOrEmpty()) {
                    Message(Shortcut.AE, "argument mismatch")
                } else {
                    when (User.checkToken(author, token)) {
                        Shortcut.OK -> {
                            val message = create(type, author, title, introduction, content, tag, timestamp)
                            val blog = message.data
                            if (message.shortcut == Shortcut.OK && blog != null) {
                                Log.createBlog(author, timestamp, ip, true, blog.blogId)
                                if (multipleForm.saveFiles(CONF.conf.blog + "/" + blog.blogId)) {
                                    message
                                } else {
                                    Message(Shortcut.OK, "some pictures may not saved", blog)
                                }
                            } else {
                                Message(Shortcut.OTHER, "fail to create blog")
                            }
                        }

                        Shortcut.TE -> {
                            Message(Shortcut.TE, "invalid token")
                        }

                        Shortcut.UNE -> {
                            Message(Shortcut.UNE, "user $author not found")
                        }

                        else -> {
                            Message(Shortcut.OTHER, "error when checking the user")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Message(Shortcut.OTHER, "unknown error")
            } finally {
                multipleForm.close()
            }
        }

        // checked
        private fun getOutlineById(id: String, blogStatus: BlogStatus = BlogStatus.NORMAL): Outline? {
            try {
                val conn = MySQLConn.connection
                val ps = conn.prepareStatement("select * from blog where blog_id = ? and status = ? limit 1")
                ps.setString(1, id)
                ps.setInt(2, blogStatus.value())
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val author = rs.getString("author_id")
                    val nickname = User.getNicknameById(author)
                    val blogId = rs.getString("blog_id")
                    val type = rs.getInt("type")
                    val tag = rs.getString("tag")
                    val lastActiveTime = rs.getTimestamp("last_active_time")
                    val introduction = rs.getString("introduction")
                    val title = rs.getString("title")
                    val status = rs.getInt("status")
                    rs.close()
                    ps.close()
                    return Outline(blogId, type, author, title, introduction, tag, lastActiveTime, nickname ?: Value.DEFAULT_NICKNAME, status)
                } else {
                    rs.close()
                    ps.close()
                    return null
                }
            } catch (e: Exception) {
                return null
            }
        }

        fun getBlogListByTime(time: Date, topic: String, count: Int, blogStatus: BlogStatus = BlogStatus.NORMAL): Message<ArrayList<Outline>> {
            val list = ArrayList<Outline>()

            try {
                val conn = MySQLConn.connection
                val timestamp = Timestamp(time.time)
                val ps = conn.prepareStatement("select blog_id, author_id, type, title, introduction, tag, last_active_time, status from blog where last_active_time <= ? and status = ? and tag like ? order by last_active_time desc limit ?")
                ps.setTimestamp(1, timestamp)
                ps.setInt(2, blogStatus.value())
                ps.setString(3, if(topic.isEmpty()) {"%%"} else {topic})
                ps.setInt(4, count)
                val rs = ps.executeQuery()
                while (rs.next()) {
                    val author = rs.getString("author_id")
                    val nickname = User.getNicknameById(author)
                    val blogId = rs.getString("blog_id")
                    val type = rs.getInt("type")
                    val tag = rs.getString("tag")
                    val lastActiveTime = rs.getTimestamp("last_active_time")
                    val introduction = rs.getString("introduction")
                    val title = rs.getString("title")
                    val status = rs.getInt("status")
                    val outline = Outline(blogId, type, author, title, introduction, tag, lastActiveTime, nickname ?: Value.DEFAULT_NICKNAME, status).parse()
                    list.add(outline)
                }
                rs.close()
                ps.close()
                return Message(Shortcut.OK, "get blog list by time successfully", list)
            } catch (e: Exception) {
                e.printStackTrace()
                return Message(Shortcut.OTHER, "unknown error")
            }
        }

        fun getBlogListById(id: String, topic: String, count: Int, blogStatus: BlogStatus = BlogStatus.NORMAL): Message<ArrayList<Outline>> {
            val list = ArrayList<Outline>()
            try {
                val conn = MySQLConn.connection
                getOutlineById(id, blogStatus)?.let {
                    val timestamp = Timestamp(it.lastActiveTime.time)
                    val ps = conn.prepareStatement("select blog_id, author_id, type, title, introduction, tag, last_active_time, status from blog where last_active_time < ? and status = ? and tag like ? order by last_active_time desc limit ?")
                    ps.setTimestamp(1, timestamp)
                    ps.setInt(2, blogStatus.value())
                    ps.setString(3, if(topic.isEmpty()) {"%%"} else {topic})
                    ps.setInt(4, count)
                    val rs = ps.executeQuery()
                    while (rs.next()) {
                        val author = rs.getString("author_id")
                        val nickname = User.getNicknameById(author)
                        val blogId = rs.getString("blog_id")
                        val type = rs.getInt("type")
                        val tag = rs.getString("tag")
                        val lastActiveTime = rs.getTimestamp("last_active_time")
                        val introduction = rs.getString("introduction")
                        val title = rs.getString("title")
                        val status = rs.getInt("status")
                        val outline = Outline(blogId, type, author, title, introduction, tag, lastActiveTime, nickname ?: Value.DEFAULT_NICKNAME, status).parse()
                        list.add(outline)
                    }
                    rs.close()
                    ps.close()
                    return Message(Shortcut.OK, "get blog list by id successfully", list)
                }
                return Message(Shortcut.BNE, "blog $id not found")
            } catch (e: Exception) {
                e.printStackTrace()
                return Message(Shortcut.OTHER, "unknown error")
            }

        }

        // checked
        fun getDetailById(blogId: String, blogStatus: BlogStatus = BlogStatus.NORMAL): Message<Detail> {
            try {
                val conn = MySQLConn.connection
                val ps = conn.prepareStatement("select * from blog where blog_id = ? and status = ? limit 1")
                ps.setString(1, blogId)
                ps.setInt(2, blogStatus.value())
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val author = rs.getString("author_id")
                    val type = rs.getInt("type")
                    val title = rs.getString("title")
                    val introduction = rs.getString("introduction")
                    val content = rs.getBlob("content").string()
                    val tag = rs.getString("tag")
                    val comments = rs.getBlob("comments").string()
                    val likes = rs.getBlob("likes").string()
                    val lastEditTime = rs.getTimestamp("last_edit_time")
                    val lastActiveTime = rs.getTimestamp("last_active_time")
                    val status = rs.getInt("status")
                    val nickname = User.getNicknameById(author) ?: Value.DEFAULT_NICKNAME

                    val detail = Detail(blogId, type, author, title, introduction, tag, lastActiveTime, nickname, status, content, comments, likes, lastEditTime)
                    rs.close()
                    ps.close()

                    return Message(Shortcut.OK, "get blog successfully", detail)
                } else {
                    rs.close()
                    ps.close()
                    return Message(Shortcut.BNE, "blog $blogId not found")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return Message(Shortcut.OTHER, "unknown error")
            }

        }

        // checked
        fun log(blogId: String, log: String) {
            try {
                val conn = MySQLConn.connection
                val ps = conn.prepareStatement("update blog set log = concat(?, log) where blog_id = ?")
                ps.setString(1, log)
                ps.setString(2, blogId)
                ps.executeUpdate()
                ps.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    enum class BlogType {
        Short, Pro, Other;

        companion object {
            fun String?.parse() = when (this) {
                "0", "Short" -> Short
                "1", "Pro" -> Pro
                else -> Other
            }
        }

        fun string() = when (this) {
            Short -> "0"
            Pro -> "1"
            Other -> "-1"
        }

        fun value() = when (this) {
            Short -> 0
            Pro -> 1
            Other -> -1
        }
    }
    enum class BlogStatus {
        NORMAL, DELETED, HIGH_PERMISSION, ALL;
        fun value() = when(this) {
            NORMAL -> 0
            DELETED -> -1
            HIGH_PERMISSION -> 2
            ALL -> 3
        }

        fun string() = value().toString()

        companion object {
            fun String.parse() = when(this) {
                "0" -> NORMAL
                "-1" -> DELETED
                "2" -> HIGH_PERMISSION
                "3" -> ALL
                else -> NORMAL
            }
        }
    }
    enum class BlogContentType {
        JSON, HTML;
        companion object {
            fun getType(string: String?) = when(string) {
                "json" -> JSON
                else -> HTML
            }
        }
    }
    enum class BlogListType {
        TIME, ID;
        companion object {
            fun getType(string: String?) = when(string) {
                "time", "TIME" -> TIME
                "id", "ID" -> ID
                else -> null
            }
        }
    }
}
