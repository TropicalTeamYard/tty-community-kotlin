package servlet

import enums.Shortcut
import model.Message
import model.Topic
import model.User
import util.CONF
import util.Value
import util.Value.fields
import java.io.PrintWriter
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@WebServlet(name = "api_topic", urlPatterns = ["/api/topic/*"])
class ApiTopic : HttpServlet() {
    private var ip: String = "0.0.0.0"
    private lateinit var out: PrintWriter

    private fun <T> Message<T>.write() {
        out.write(this.json())
    }

    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        doPost(req, resp)
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        resp!!.characterEncoding = "utf-8"
        req!!.characterEncoding = "utf-8"
        out = resp.writer
        ip = Value.getIP(req)
        val route = try {
            req.requestURI.substring(21)
        } catch (e: StringIndexOutOfBoundsException) {
            Message(Shortcut.AE, "invalid request", null).write()
            return
        }

        val fields = req.parameterMap.fields()
        when (route) {

            "create" -> {
                /**
                 * @http http://47.102.200.155:8080/community/api/topic/create
                 * @values id, token, name, parent, introduction
                 * @intro
                 * Only user group > 1 can create the topic, or the system will notify the message
                 * the board, and users can decide whether to create the topic, the user whose
                 * user_group > 3 can create the topic directly
                 *  Every topic must have a parent, except root topic ALL(id = "000000")
                 */

                val id = fields["id"]
                val token = fields["token"]
                val name = fields["name"]
                val parent = fields["parent"]
                val introduction = fields["introduction"]
                TODO()

            }


            /**
             * http://47.102.200.155:8080/community/api/topic/find?name=ALL
             * @field name
             * find the topic which match to the name
             */
            "find" -> find(fields["name"]).write()

            /**
             * http://47.102.200.155:8080/community/api/topic/similar?name=al
             * find the list of topic whose name or introduction similar to the name for user to select
             */
            "similar" -> similar(fields["name"]).write()

            /**
             * http://47.102.200.155:8080/community/api/topic/list?id=2008153477
             * @field id
             * the following topic list of the user's id, the info is public for everyone
             */
            "list" -> list(fields["id"]).write()

            /**
             * http://47.102.200.155:8080/community/api/topic/parent?id=000001
             * @field id
             */
            "parent" -> parent(fields["id"]).write()

            /**
             * http://47.102.200.155:8080/community/api/topic/child?id=000001
             * @field id
             */
            "child" -> child(fields["id"]).write()

            /**
             * http://localhost:8080/community/api/topic/follow?id=1554751432&topic=000001&token=B240A5EE666017D53146C4FD404F2136
             * @field id, token, topic_id
             * the action that the user want to follow the topic that he can view the
             * content of the topic, the id and token is necessary to ensure the request
             * is posted by user himself
             */
            "follow" -> follow(fields["id"], fields["token"], fields["topic"]).write()

            "unfollow" -> {
                val userId = fields["id"]
                val token = fields["token"]
                val topicId = fields["topic"]
                TODO()
            }

            "info" -> {
                TODO()
            }

            "picture" -> {
                TODO()
            }

            "change_picture" -> {
                TODO()
            }

            "change_info" -> {
                TODO()
            }


            /**
             * http://47.102.200.155:8080/community/api/topic/test
             */
            "test" -> test()

            else -> Message(Shortcut.AE, "invalid request.", null).write()
        }
    }


    private fun unfollow(userId: String?, token: String?, topicId: String?): Message<Any> {
        if (userId.isNullOrEmpty() || token.isNullOrEmpty() || topicId.isNullOrEmpty()) {
            return Message(Shortcut.AE, "argument mismatch")
        } else if (topicId == "000000") {
            return Message(Shortcut.OK, "nothing changed")
        } else {
            return when(val shortcut = User.checkToken(userId, token)) {
                Shortcut.OK -> {
                    val topic = Topic.getDetailById(topicId)
                    if (topic != null) {
                        Message(Topic.removeFollowerById(userId, topic), "")
                    } else {
                        Message(Shortcut.TNE, "topic $topicId not found")
                    }
                }

                else -> Message(shortcut, "check user failed")
            }
        }
    }

    // checked
    private fun follow(userId: String?, token: String?, topicId: String?): Message<Any> {
        if (userId.isNullOrEmpty() || token.isNullOrEmpty() || topicId.isNullOrEmpty()) {
            return Message(Shortcut.AE, "argument mismatch")
        } else if (topicId == "000000") {
            return Message(Shortcut.OK, "nothing changed")
        } else {
            return when(val shortcut = User.checkToken(userId, token)) {
                Shortcut.OK -> {
                    val topic = Topic.getDetailById(topicId)
                    if (topic != null) {
                        Message(Topic.addFollowerById(userId, topic), "...")
                    } else {
                        Message(Shortcut.TNE, "topic $topicId not found")
                    }
                }

                else -> Message(shortcut, "check user failed")
            }
        }
    }

    private fun create(id: String, token: String, name: String, parent: String, introduction: String): Shortcut {
        TODO()
    }

    // checked
    private fun list(id: String?): Message<ArrayList<Topic.Outline>> {
        return if(id.isNullOrEmpty()) {
            Message(Shortcut.AE, "argument mismatch")
        } else {
            val outlines = User.topic(id)
            if (outlines != null) {
                Message(Shortcut.OK, "ok", outlines)
            } else {
                Message(Shortcut.OTHER, "unknown error")
            }
        }
    }

    // checked
    private fun find(name: String?): Message<Topic.Outline> {
        return if (name.isNullOrEmpty()) {
            Message(Shortcut.AE, "argument mismatch")
        } else {
            findTopicOutlineByName(name, FindOutlineType.EQUALS)
        }
    }

    // checked
    private fun parent(id: String?): Message<Topic.Outline> {
        return if (id.isNullOrEmpty()) {
            Message(Shortcut.AE, "")
        } else {
            findTopicOutlineById(id, FindOutlineType.Parent)
        }
    }

    // checked
    private fun similar(name: String?): Message<ArrayList<Topic.Outline>> {
        return if (name.isNullOrEmpty()) {
            Message(Shortcut.AE, "argument mismatch")
        } else {
            findTopicOutlineListByName(name, FindOutlineListType.LIKE)
        }
    }

    // checked
    private fun child(id: String?): Message<ArrayList<Topic.Outline>> {
        return if (id.isNullOrEmpty()) {
            Message(Shortcut.AE, "")
        } else {
            findTopicOutlineListById(id, FindOutlineListType.Child)
        }
    }

    // checked
    private fun findTopicOutlineListById(id: String, type: FindOutlineListType): Message<ArrayList<Topic.Outline>> {
        val topic = Topic.findOutlineById(id)
        return if (topic != null) {
            findTopicOutlineListByName(topic.name, type)
        } else {
            Message(Shortcut.TNE, "topic $id not found")
        }
    }

    // checked
    private fun findTopicOutlineListByName(name: String, type: FindOutlineListType): Message<ArrayList<Topic.Outline>> {
        return try {
            when (type) {
                FindOutlineListType.LIKE -> Message(Shortcut.OK, "", Topic.similarTopic(name))
                FindOutlineListType.Child -> {
                    val info = Topic.findChildrenByName(name)
                    if (info != null) {
                        Message(Shortcut.OK, "", info)
                    } else {
                        Message(Shortcut.TNE, "topic $name not found")
                    }
                }
            }
        } catch (e: Exception) {
            Message(Shortcut.OTHER, "unknown error")
        }

    }

    // checked
    private fun test() {
        out.println(CONF.root)
        out.println(CONF.conf.server)
    }

    // checked
    private fun findTopicOutlineByName(name: String, type: FindOutlineType): Message<Topic.Outline> {
        return try {
            val topic = when (type) {
                FindOutlineType.EQUALS -> Topic.findOutlineByName(name)
                FindOutlineType.Parent -> Topic.findParentByName(name)
            }
            if (topic != null) {
                Message(Shortcut.OK, "ok", topic)
            } else {
                Message(Shortcut.TNE, "topic $name not found")
            }
        } catch (e: Exception) {
            Message(Shortcut.OTHER, "unknown error")
        }

    }

    // checked
    private fun findTopicOutlineById(id: String, type: FindOutlineType): Message<Topic.Outline> {
        val topic = Topic.findOutlineById(id)
        return if (topic != null) {
            findTopicOutlineByName(topic.name, type)
        } else {
            Message(Shortcut.TNE, "topic $id not found")
        }
    }

    enum class FindOutlineType {
        EQUALS, Parent
    }
    enum class FindOutlineListType {
        LIKE, Child
    }

}