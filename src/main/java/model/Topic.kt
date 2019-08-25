package model

import com.google.gson.reflect.TypeToken
import enums.Shortcut
import util.CONF
import util.CONF.Companion.gson
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
        val follower: ArrayList<String> = CONF.gson.fromJson(follower, object : TypeToken<ArrayList<String>>(){}.type)
        val lastActiveTime: Date = Date(lastActiveTime.time)
    }

    companion object {

        // check
        fun similarTopic(name: String): ArrayList<Outline> {
            val list = ArrayList<Outline>()
            try {
                val conn = MySQLConn.connection
                val ps =
                    conn.prepareStatement("select topic_id, name, parent, introduction from topic where name like ? or introduction like ?")
                ps.setString(1, "%$name%")
                ps.setString(2, "%$name%")

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
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return list
        }

        // checked
        fun findOutlineByName(name: String): Outline? {
            val conn = MySQLConn.connection
            val ps = conn.prepareStatement("select topic_id, name, parent, introduction from topic where name = ? limit 1")
            ps.setString(1, name)
            val rs = ps.executeQuery()
            return if (rs.next()) {
                val id = rs.getString("topic_id")
                val parent = rs.getString("parent")
                val introduction = rs.getString("introduction")
                rs.close()
                ps.close()
                Outline(id, name, parent, introduction)
            } else {
                rs.close()
                ps.close()
                null
            }
        }

        // checked
        fun findOutlineById(id: String): Outline? {
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
                return null
            }
        }

        // checked
        fun findParentById(childId: String): Outline? {
            val childTopic = findOutlineById(childId)
            return childTopic?.parent?.let { findOutlineById(it) }
        }

        // checked
        fun findParentByName(chileName: String): Outline? {
            val childTopic = findOutlineByName(chileName)
            return childTopic?.parent?.let { findOutlineById(it) }
        }

        // checked
        private fun findChildrenById(parentId: String): ArrayList<Outline>? {
            val parentTopic = findOutlineById(parentId)
            val list = ArrayList<Outline>()
            if(parentTopic != null) {
                val conn = MySQLConn.connection
                val ps = conn.prepareStatement("select topic_id, name, parent, introduction from topic where parent = ?")
                ps.setString(1, parentTopic.id)
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
            } else {
                return null
            }
        }

        // checked
        fun findChildrenByName(parentName: String): ArrayList<Outline>? {
            val parentTopic = findOutlineByName(parentName)
            return parentTopic?.id?.let { findChildrenById(it) }
        }

        // checked
        fun getDetailById(id: String): Detail? {
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
                val follower = rs.getString("follower")
                val admin = rs.getString("admin")
                val lastActiveTime = rs.getTimestamp("last_active_time")
                val status = rs.getString("status")
                rs.close()
                ps.close()
                return Detail(id, name, parent, introduction, picture, follower, admin, status, lastActiveTime)
            } else {
                rs.close()
                ps.close()
                return null
            }
        }

        // checked
        fun getDetailByName(name: String): Detail? {
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
                val follower = rs.getString("follower")
                val admin = rs.getString("admin")
                val lastActiveTime = rs.getTimestamp("last_active_time")
                val status = rs.getString("status")
                rs.close()
                ps.close()
                return Detail(id, name, parent, introduction, picture, follower, admin, status, lastActiveTime)
            } else {
                rs.close()
                ps.close()
                return null
            }
        }

        fun addFollowerById(userId: String, topic: Detail): Shortcut {
            try {
                val conn = MySQLConn.connection

                val ps1 = conn.prepareStatement("update topic set follower = JSON_ARRAY_INSERT(follower, '$[0]', ?) where not json_contains(follower, json_array(?)) and topic_id = ?")
                ps1.setString(1, userId)
                ps1.setString(2, userId)
                ps1.setString(3, topic.id)
                ps1.execute()
                ps1.close()
                val ps2 = conn.prepareStatement("update user_detail set topic = JSON_ARRAY_INSERT(topic, '$[0]', ?) where not json_contains(topic, json_array(?)) and id = ?")
                ps2.setString(1, topic.id)
                ps2.setString(2, topic.id)
                ps2.setString(3, userId)
                ps2.execute()
                ps2.close()
                return Shortcut.OK

            } catch (e: Exception) {
                return Shortcut.OTHER
            }

        }

        fun removeFollowerById(userId: String, topic: Detail): Shortcut {
            return Shortcut.PME
        }

        fun log(topicId: String, log: String) {
            try {
                val conn = MySQLConn.connection
                val ps = conn.prepareStatement("update topic set log = concat(?, log) where topic_id = ?")
                ps.setString(1, log)
                ps.setString(2, topicId)
                ps.executeUpdate()
                ps.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
