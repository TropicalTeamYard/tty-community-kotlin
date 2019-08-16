package model

import com.google.gson.Gson
import exception.ShortcutThrowable
import model.Blog.Companion.Type.Companion.value
import util.conn.MySQLConn
import java.sql.SQLException
import java.sql.Timestamp
import java.util.*

interface Blog {
    val blogId: String
    val type: String
    val author: String
    val nickname: String
    val title: String
    val introduction: String
    val tag: String
    val lastActiveTime: Date

    class Outline(
        override val blogId: String,
        override val type: String,
        override val author: String,
        override val title: String,
        override val introduction: String,
        override val tag: String,
        override val lastActiveTime: Date,
        override val nickname: String
    ) : Blog {
        var index = -1
    }

    class Detail(
        override val blogId: String,
        override val type: String,
        override val author: String,
        override val title: String,
        override val introduction: String,
        override val tag: String,
        override val lastActiveTime: Date,
        override val nickname: String,
        var content: String,
        comment: String,
        likes: String,
        var status: String,
        var lastEditTime: Date
    ) : Blog {
        val comment: Comment = gson.fromJson(comment, Comment::class.java)
        val likes: String = gson.fromJson(likes, String::class.java)
    }

    class Comment(val id: String, val nickname: String, val time: String)

    companion object {
        val gson = Gson()

        enum class Type {
            Short, Pro, Other;

            companion object {
                val Type.value: Int
                    get() {
                        return when (this) {
                            Short -> 0
                            Pro -> 1
                            Other -> -1
                        }
                    }

                val String?.parse: Type
                    get() {
                        return when (this) {
                            "0", "Short" -> {
                                Short
                            }

                            "1", "Pro" -> {
                                Pro
                            }

                            else -> {
                                Other
                            }
                        }
                    }
            }
        }

        @Throws(ShortcutThrowable::class)
        fun create(
            type: Type,
            author: String,
            title: String,
            introduction: String,
            content: String,
            tag: String,
            time: Timestamp
        ): Outline {
            try {
                val conn = MySQLConn.connection
                val blogId =
                    ("$author$content${time.time}$tag${(1000..9999).random()}".hashCode() and Integer.MAX_VALUE).toString()
                val ps =
                    conn.prepareStatement("insert into blog (blog_id, type, author_id, title, introduction, content, tag, last_edit_time, last_active_time, status, data, log, comment, likes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                ps.setString(1, blogId)
                ps.setInt(2, type.value)
                ps.setString(3, author)
                ps.setString(4, title)
                ps.setString(5, introduction)
                ps.setString(6, content)
                ps.setString(7, tag) // id of topic
                ps.setTimestamp(8, time) // last_edit_time
                ps.setTimestamp(9, time) // last_active_time
                ps.setString(10, "normal") // status
                ps.setString(11, "") // data
                ps.setString(12, "init\n") // log
                ps.setString(13, "") // comment
                ps.setString(14, "") // likes
                ps.execute()
                ps.close()
                return getOutlineById(blogId)
            } catch (e: SQLException) {
                e.printStackTrace()
                throw ShortcutThrowable.OTHER("SQL ERROR")
            }
        }

        @Throws(ShortcutThrowable::class)
        fun getOutlineById(id: String): Outline {
            try {
                val conn = MySQLConn.connection
                val ps = conn.prepareStatement("select * from blog where blog_id = ? limit 1")
                ps.setString(1, id)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val author = rs.getString("author_id")
                    val nickname = User.getNicknameById(author)
                    val blogId = rs.getString("blog_id")
                    val type = rs.getInt("type")
                    val tag = rs.getString("tag")
                    val lastActiveTime = rs.getTimestamp("last_active_time")
                    val introduction = rs.getString("introduction").replace("####blog_id####", blogId)
                    var title = rs.getString("title")
                    rs.close()
                    ps.close()
                    nickname?.let {
                        title = title.replace("####nickname####", it)
                        return Outline(blogId, type.toString(), author, title, introduction, tag, lastActiveTime, it)
                    }
                    throw ShortcutThrowable.UNE()
                } else {
                    rs.close()
                    ps.close()
                    throw ShortcutThrowable.BNE()
                }
            } catch (e: SQLException) {
                throw ShortcutThrowable.OTHER("SQL ERROR")
            }
        }

        fun log(blogId: String, log: String) {
            val conn = MySQLConn.connection
            val ps = conn.prepareStatement("update blog set log = concat(?, log) where blog_id = ?")
            ps.setString(1, log)
            ps.setString(2, blogId)
            ps.executeUpdate()
            ps.close()
        }
    }
}
