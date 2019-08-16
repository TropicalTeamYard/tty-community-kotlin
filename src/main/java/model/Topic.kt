package model

import exception.ShortcutThrowable
import util.Value.string
import util.conn.MySQLConn
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList

interface Topic {
    val id: String
    val name: String
    val parent: String
    val introduction: String

    class Outline(
        override val id: String,
        override val name: String,
        override val parent: String,
        override val introduction: String
    ) : Topic

    class Detail(
        override val id: String,
        override val name: String,
        override val parent: String,
        override val introduction: String,
        val picture: String,
        follower: String,
        val admin: String,
        val status: String,
        lastActiveTime: Timestamp
    ) : Topic {
        val follower: List<String> = follower.split("|")
        val lastActiveTime: Date = Date(lastActiveTime.time)
    }

    companion object {
        fun similarTopic(name: String): ArrayList<Outline> {
            val conn = MySQLConn.connection
            val ps =
                conn.prepareStatement("select topic_id, name, parent, introduction from topic where name like ? or introduction like ?")
            ps.setString(1, "%$name%")
            ps.setString(2, "%$name%")
            val list = ArrayList<Outline>()
            val rs = ps.executeQuery()
            while (rs.next()) {
                val id = rs.getString("topic_id")
                val n = rs.getString("name")
                val parent = rs.getString("parent")
                val introduction = rs.getString("introduction")
                val topic = Outline(id, n, parent, introduction)
                list.add(topic)
            }
            rs.close()
            ps.close()
            return list
        }

        @Throws(ShortcutThrowable::class)
        fun findOutlineByName(name: String): Outline {
            val conn = MySQLConn.connection
            val ps =
                conn.prepareStatement("select topic_id, name, parent, introduction from topic where name = ? limit 1")
            ps.setString(1, name)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val id = rs.getString("topic_id")
                val parent = rs.getString("parent")
                val introduction = rs.getString("introduction")
                rs.close()
                ps.close()
                return Outline(id, name, parent, introduction)
            } else {
                rs.close()
                ps.close()
                throw ShortcutThrowable.TNE("topic $name not found")
            }
        }

        @Throws(ShortcutThrowable::class)
        fun findOutlineById(id: String): Outline {
            val conn = MySQLConn.connection
            val ps =
                conn.prepareStatement("select topic_id, name, parent, introduction from topic where topic_id = ? limit 1")
            ps.setString(1, id)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val name = rs.getString("name")
                val parent = rs.getString("parent")
                val introduction = rs.getString("introduction")
                rs.close()
                ps.close()
                return Outline(id, name, parent, introduction)
            } else {
                rs.close()
                ps.close()
                throw ShortcutThrowable.TNE("topic $id not found")
            }
        }

        @Throws(ShortcutThrowable::class)
        fun findParentById(childId: String): Outline {
            val childTopic = findOutlineById(childId)
            return findOutlineById(childTopic.parent)
        }

        @Throws(ShortcutThrowable::class)
        fun findParentByName(chileName: String): Outline {
            val childTopic = findOutlineByName(chileName)
            return findOutlineById(childTopic.parent)
        }

        @Throws(ShortcutThrowable::class)
        fun findChildrenById(parentId: String): ArrayList<Outline> {
            val parentTopic = findOutlineById(parentId)

            val conn = MySQLConn.connection
            val ps = conn.prepareStatement("select topic_id, name, parent, introduction from topic where parent = ?")
            ps.setString(1, parentTopic.id)
            val list = ArrayList<Outline>()
            val rs = ps.executeQuery()
            while (rs.next()) {
                val childId = rs.getString("topic_id")
                val name = rs.getString("name")
                val parent = rs.getString("parent")
                val introduction = rs.getString("introduction")
                val child = Outline(childId, name, parent, introduction)
                list.add(child)
            }
            rs.close()
            ps.close()
            return list
        }

        @Throws(ShortcutThrowable::class)
        fun findChildrenByName(parentName: String): ArrayList<Outline> {
            val parentTopic = findOutlineByName(parentName)
            return findChildrenById(parentTopic.id)
        }

        @Throws(ShortcutThrowable::class)
        fun getDetailById(id: String): Detail {
            val conn = MySQLConn.connection
            val ps =
                conn.prepareStatement("select name, parent, introduction, picture, follower, admin, last_active_time, status from topic where topic_id = ? limit 1")
            ps.setString(1, id)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val name = rs.getString("name")
                val parent = rs.getString("parent")
                val introduction = rs.getString("introduction")
                val picture = rs.getString("picture")
                val follower = rs.getBlob("follower").string()
                val admin = rs.getString("admin")
                val lastActiveTime = rs.getTimestamp("last_active_time")
                val status = rs.getString("status")
                rs.close()
                ps.close()
                return Detail(id, name, parent, introduction, picture, follower, admin, status, lastActiveTime)
            } else {
                rs.close()
                ps.close()
                throw ShortcutThrowable.TNE("topic $id not found")
            }
        }

        @Throws(ShortcutThrowable::class)
        fun getDetailByName(name: String): Detail {
            val conn = MySQLConn.connection
            val ps =
                conn.prepareStatement("select topic_id, parent, introduction, picture, follower, admin, last_active_time, status from topic where name = ? limit 1")
            ps.setString(1, name)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val id = rs.getString("topic_id")
                val parent = rs.getString("parent")
                val introduction = rs.getString("introduction")
                val picture = rs.getString("picture")
                val follower = rs.getBlob("follower").string()
                val admin = rs.getString("admin")
                val lastActiveTime = rs.getTimestamp("last_active_time")
                val status = rs.getString("status")
                rs.close()
                ps.close()
                return Detail(id, name, parent, introduction, picture, follower, admin, status, lastActiveTime)
            } else {
                rs.close()
                ps.close()
                throw ShortcutThrowable.TNE("topic $name not found")
            }
        }

        fun log(topicId: String, log: String) {
            val conn = MySQLConn.connection
            val ps = conn.prepareStatement("update topic set log = concat(?, log) where topic_id = ?")
            ps.setString(1, log)
            ps.setString(2, topicId)
            ps.executeUpdate()
            ps.close()
        }
    }

}
