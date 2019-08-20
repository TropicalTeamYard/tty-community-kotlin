package model

import com.google.gson.reflect.TypeToken
import enums.Shortcut
import util.CONF
import util.Value
import util.Value.string
import util.conn.MySQLConn
import util.parse.MultipleForm
import java.sql.SQLException
import java.sql.Timestamp
import java.util.*
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest
import kotlin.collections.ArrayList

class User {

    class LoginResult(var id: String, var nickname: String, val email: String, var token: String)

    class Updater(var id: String, var token: String, var ip: String) {
        private val shortcut = checkToken(id, token)
        private val conn = MySQLConn.connection
        private val items = ArrayList<Item>()
        var date = Date()
        private var before: Message<PrivateInfo> = PrivateInfo.get(id, token)

        fun add(item: Item) {
            items.add(item)
        }

        fun result(): Message<ArrayList<Item>> {
            val after: Message<PrivateInfo> = PrivateInfo.get(id, token)
            return when(after.shortcut) {
                Shortcut.OK -> {
                    Log.changeUserInfo(id,date, ip, before.data, after.data)
                    Message(Shortcut.OK, "success change info", items)
                }
                Shortcut.TE -> Message(Shortcut.TE, "invalid token")
                Shortcut.UNE -> Message(Shortcut.UNE, "user $id not found")
                else -> Message(Shortcut.OTHER, "error when checking the token")
            }
        }

        data class Item(val key: String, val value: String, val status: Shortcut)

        fun nickname(value: String): Item {
            val key = "nickname"
            return if (shortcut != Shortcut.OK) Item(key, value, shortcut)
            else if (!((2..15).contains(value.length) && value.isNicknameValid())) Item(key, value, Shortcut.AIF)
            else if (value.exist()) Item(key, value, Shortcut.UR)
            else try {
                val ps = conn.prepareStatement("update user set nickname = ? where id = ?")
                ps.setString(1, value)
                ps.setString(2, id)
                val effect = ps.executeUpdate()
                ps.close()
                if (effect > 0) {
                    Item(key, value, Shortcut.OK)
                } else {
                    Item(key, value, Shortcut.UNE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Item(key, value, Shortcut.OTHER)
            }
        }

        fun email(value: String): Item {
            val key = "email"
            return if (shortcut != Shortcut.OK) Item(key, value, shortcut)
            else if (!value.isEmailValid()) Item(key, value, Shortcut.AIF)
            else try {
                val ps = conn.prepareStatement("update user set email = ? where id = ?")
                ps.setString(1, value)
                ps.setString(2, id)
                val effect = ps.executeUpdate()
                ps.close()
                if (effect > 0) {
                    Item(key, value, Shortcut.OK)
                } else {
                    Item(key, value, Shortcut.UNE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Item(key, value, Shortcut.OTHER)
            }
        }

        fun signature(value: String): Item {
            val key = "signature"
            return if (shortcut != Shortcut.OK) Item(key, value, shortcut)
            else if (!(2..20).contains(value.length)) Item(key, value, Shortcut.AIF)
            else try {
                val ps = conn.prepareStatement("update user_detail set personal_signature = ? where id = ?")
                ps.setString(1, value)
                ps.setString(2, id)
                val effect = ps.executeUpdate()
                ps.close()
                if (effect > 0) {
                    Item(key, value, Shortcut.OK)
                } else {
                    Item(key, value, Shortcut.UNE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Item(key, value, Shortcut.OTHER)
            }
        }

        fun school(value: String): Item {
            val key = "school"
            return if (shortcut != Shortcut.OK) Item(key, value, shortcut)
            else if (!value.isSchoolValid()) Item(key, value, Shortcut.AIF)
            else try {
                val ps = conn.prepareStatement("update user_detail set school = ? where id = ?")
                ps.setString(1, value)
                ps.setString(2, id)
                val effect = ps.executeUpdate()
                ps.close()
                if (effect > 0) {
                    Item(key, value, Shortcut.OK)
                } else {
                    Item(key, value, Shortcut.UNE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Item(key, value, Shortcut.OTHER)
            }
        }
    }

    class PrivateInfo(
        override val id: String,
        override val nickname: String,
        override val email: String,
        override val portrait: String,
        override val signature: String,
        override val userGroup: Int,
        override val exp: Int,
        override val school: String
    ) : SimpleUser, SimpleDetail {

        constructor(user: SimpleUser.User, detail: SimpleDetail.Detail) : this(
            user.id,
            user.nickname,
            user.email,
            detail.portrait,
            detail.signature,
            detail.userGroup,
            detail.exp,
            detail.school
        )

        companion object {
            fun get(id: String, token: String): Message<PrivateInfo> {
                when (checkToken(id, token)) {
                    Shortcut.OK -> {
                        val user = SimpleUser.get(id) ?: return Message(Shortcut.UNE, "user $id not found")
                        val detail = SimpleDetail.get(id) ?: return Message(Shortcut.UNE, "user $id not found")
                        return Message(Shortcut.OK, "success", PrivateInfo(user, detail))
                    }
                    Shortcut.TE -> return Message(Shortcut.TE, "invalid token")
                    Shortcut.UNE -> return Message(Shortcut.UNE, "user $id not found")
                    else -> return Message(Shortcut.OTHER, "error when checking the token")
                }
            }
        }
    }

    class PublicInfo(
        val id: String,
        val nickname: String,
        val email: String,
        val portrait: String,
        follower: String,
        following: String,
        val signature: String,
        val exp: Int,
        topic: String,
        val school: String
    ) {
        val follower: ArrayList<String> = gson.fromJson(follower, object : TypeToken<ArrayList<String>>(){}.type)
        val following: ArrayList<String> = gson.fromJson(following, object : TypeToken<ArrayList<String>>(){}.type)
        val topic: ArrayList<String> = gson.fromJson(topic, object : TypeToken<ArrayList<String>>(){}.type)

        companion object {
            // checked
            fun get(id: String?): Message<PublicInfo> {
                if (id.isNullOrEmpty()) {
                    return Message(Shortcut.AE, "argument mismatch")
                } else {
                    val message: Message<PublicInfo>
                    val conn = MySQLConn.connection
                    try {
                        val ps0 = conn.prepareStatement("select nickname, email from user where id = ? limit 1")
                        ps0.setString(1, id)
                        val rs0 = ps0.executeQuery()
                        if (rs0.next()) {
                            val nickname = rs0.getString("nickname")
                            val email = rs0.getString("email")
                            val ps1 =
                                conn.prepareStatement("select portrait, follower, following, personal_signature, exp, topic, school from user_detail where id = ? limit 1")
                            ps1.setString(1, id)
                            val rs1 = ps1.executeQuery()
                            if (rs1.next()) {
                                val portrait = rs1.getString("portrait")
                                val follower = rs1.getBlob("follower").string()
                                val following = rs1.getBlob("following").string()
                                val signature = rs1.getString("personal_signature")
                                val exp = rs1.getInt("exp")
                                val topic = rs1.getBlob("topic").string()
                                val school = rs1.getString("school")
                                message = Message(
                                    Shortcut.OK,
                                    "ok",
                                    PublicInfo(id, nickname, email, portrait, follower, following, signature, exp, topic, school)
                                )
                            } else {
                                message = Message(Shortcut.UNE, "user $id not found")
                            }
                            rs1.close()
                            ps1.close()
                        } else {
                            message = Message(Shortcut.UNE, "user $id not found")
                        }
                        rs0.close()
                        ps0.close()

                        return message
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return Message(Shortcut.OTHER, "unknown error")
                    }
                }
            }
        }

    }

    interface SimpleUser {
        val id: String
        val nickname: String
        val email: String

        class User(override val id: String, override val nickname: String, override val email: String) : SimpleUser

        companion object {
            fun get(id: String): User? {
                var user: User? = null
                try {
                    val conn = MySQLConn.connection
                    val ps = conn.prepareStatement("select nickname, email from user where id = ? limit 1")
                    ps.setString(1, id)
                    val rs = ps.executeQuery()
                    if (rs.next()) {
                        val nickname = rs.getString("nickname")
                        val email = rs.getString("email")
                        user = User(id, nickname, email)
                    }
                    rs.close()
                    ps.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return user
            }
        }
    }

    interface SimpleDetail {
        val portrait: String
        val signature: String
        val userGroup: Int
        val exp: Int
        val school: String

        class Detail(
            override val portrait: String,
            override val signature: String,
            override val userGroup: Int,
            override val exp: Int,
            override val school: String
        ) : SimpleDetail

        companion object {
            fun get(id: String): Detail? {
                var detail: Detail? = null
                try {
                    val conn = MySQLConn.connection
                    val ps = conn.prepareStatement("select portrait, personal_signature, user_group, exp, school from user_detail where id = ? limit 1")
                    ps.setString(1, id)
                    val rs = ps.executeQuery()
                    if (rs.next()) {
                        val portrait = rs.getString("portrait")
                        val signature = rs.getString("personal_signature")
                        val userGroup = rs.getInt("user_group")
                        val exp = rs.getInt("exp")
                        val school = rs.getString("school")
                        detail = Detail(portrait, signature, userGroup, exp, school)
                    }
                    rs.close()
                    ps.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return detail
            }
        }
    }

    companion object {
        // checked
        private fun getToken(id: String, platform: LoginPlatform, secret: String, time: Date, status: Boolean): String {
            return "$id::${platform.name}::$secret::${Value.getTime(time)}::$status"
        }

        // checked
        fun getNicknameById(id: String): String? {
            val conn = MySQLConn.connection
            var nickname: String?
            try {
                val ps = conn.prepareStatement("select nickname from user where id = ? limit 1")
                ps.setString(1, id)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    nickname = rs.getString("nickname")
                } else {
                    nickname = null
                }
                rs.close()
                ps.close()
            } catch (e: SQLException) {
                e.printStackTrace()
                nickname = null
            }

            return nickname
        }

        // checked
        private fun getIdByNickname(nickname: String): String? {
            val conn = MySQLConn.connection
            var id: String?
            try {
                val ps = conn.prepareStatement("select id from user where nickname = ? limit 1")
                ps.setString(1, nickname)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    id = rs.getString("id")
                } else {
                    id = null
                }
                rs.close()
                ps.close()
            } catch (e: SQLException) {
                e.printStackTrace()
                id = null
            }

            return id
        }

        // checked
        fun String.exist(): Boolean {
            val conn = MySQLConn.connection
            try {
                val ps = conn.prepareStatement("select * from user where nickname = ?")
                ps.setString(1, this)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    rs.close()
                    ps.close()
                    return true
                }
                rs.close()
                ps.close()
                // nick can be registered
                return false
            } catch (e: SQLException) {
                e.printStackTrace()
                return true
            }
        }

        // checked
        fun String.isNicknameValid(): Boolean {
            return Pattern.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5]+$", this)
        }

        // checked
        fun String.isEmailValid(): Boolean {
            return Pattern.matches("^[A-Za-z0-9\\u4e00-\\u9fa5]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+\$", this)
        }

        // todo
        fun String.isSchoolValid(): Boolean {
            // todo check the school name valid
            return (3..15).contains(this.length)
        }

        // checked
        fun checkToken(id: String, token: String): Shortcut {
            val conn = MySQLConn.connection
            try {
                val ps = conn.prepareStatement("select * from user where id = ? limit 1")
                ps.setString(1, id)
                val rs = ps.executeQuery()
                return if (rs.next()) {
                    if (Value.getMD5(rs.getString("token")) == token) {
                        rs.close()
                        ps.close()
                        Shortcut.OK
                    } else {
                        rs.close()
                        ps.close()
                        Shortcut.TE
                    }
                } else {
                    ps.close()
                    Shortcut.UNE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return Shortcut.OTHER
            }
        }

        // checked
        private fun checkPassword(account: String, password: String, type: LoginType): Shortcut {
            val conn = MySQLConn.connection
            try {
                when (type) {
                    LoginType.ID -> {
                        val ps = conn.prepareStatement("select * from user where id = ? limit 1")
                        ps.setString(1, account)
                        val rs = ps.executeQuery()
                        return if (rs.next()) {
                            if (rs.getString("password") == password) {
                                rs.close()
                                ps.close()
                                Shortcut.OK
                            } else {
                                rs.close()
                                ps.close()
                                Shortcut.UPE
                            }
                        } else {
                            ps.close()
                            Shortcut.UNE
                        }
                    }
                    LoginType.NICKNAME -> {
                        val ps = conn.prepareStatement("select * from user where nickname = ? limit 1")
                        ps.setString(1, account)
                        val rs = ps.executeQuery()
                        return if (rs.next()) {
                            if (rs.getString("password") == password) {
                                rs.close()
                                ps.close()
                                Shortcut.OK
                            } else {
                                rs.close()
                                ps.close()
                                Shortcut.UPE
                            }
                        } else {
                            ps.close()
                            Shortcut.UNE
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                return Shortcut.OTHER
            }
        }

        // checked
        private fun updateLogin(id: String, ip: String, timestamp: Timestamp): Int {
            return try {
                val conn = MySQLConn.connection
                val ps = conn.prepareStatement("update user set last_login_ip = ?, last_login_time = ? where id = ?")
                ps.setString(1, ip)
                ps.setTimestamp(2, timestamp)
                ps.setString(3, id)
                val effect = ps.executeUpdate()
                ps.close()
                effect
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }

        // checked
        private fun updateToken(id: String, token: String): Int {
            return try {
                val conn = MySQLConn.connection
                val ps = conn.prepareStatement("update user set token = ? where id = ?")
                ps.setString(1, token)
                ps.setString(2, id)
                val effect = ps.executeUpdate()
                ps.close()
                effect
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }

        }

        // checked
        fun autoLogin(ip: String, id: String, token: String, platform: LoginPlatform): Message<SimpleUser> {
            try {
                val timestamp = Timestamp(Date().time)
                when (checkToken(id, token)) {
                    Shortcut.OK -> {
                        updateLogin(id, ip, timestamp)
                        SimpleUser.get(id)?.let {
                            Log.autoLogin(id, timestamp, ip, platform, true)
                            return Message(Shortcut.OK, "ok, let's fun", it)
                        }
                        return Message(Shortcut.UNE, "user $id not found")
                    }

                    Shortcut.TE -> {
                        Log.autoLogin(id, timestamp, ip, platform, false)
                        return Message(Shortcut.TE, "invalid token")
                    }

                    Shortcut.UNE -> {
                        return Message(Shortcut.UNE, "user $id not found")
                    }

                    else -> {
                        return Message(Shortcut.OTHER, "error when checking the token")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return Message(Shortcut.OTHER, "unknown error")
            }

        }

        // checked
        fun login(account: String, password: String, type: LoginType, platform: LoginPlatform, ip: String): Message<LoginResult> {
            val time = Date()
            val id = when (type) {
                LoginType.ID -> account
                LoginType.NICKNAME -> getIdByNickname(account)
            }
            when (checkPassword(account, password, type)) {
                Shortcut.OK -> {
                    id?.let {
                        val token = getToken(it, platform, CONF.secretKey, time, true)
                        val user = SimpleUser.get(it)
                        val effect = updateToken(it, token)
                        if (effect > 0 && user != null) {
                            val result = LoginResult(user.id, user.nickname, user.email, Value.getMD5(token))
                            Log.login(it, time, ip, type, platform, true)
                            return Message(Shortcut.OK, "ok, let's fun", result)
                        }
                    }
                    return Message(Shortcut.UNE, "user $account not found")
                }
                Shortcut.UNE -> {
                    return Message(Shortcut.UNE, "user $account not found")
                }
                Shortcut.UPE -> {
                    id?.let {
                        Log.login(it, time, ip, type, platform, false)
                    }
                    return Message(Shortcut.UPE, "wrong password")
                }
                else -> {
                    return Message(Shortcut.OTHER, "unknown error")
                }
            }
        }

        // checked
        fun register(nickname: String, ip: String, email: String, password: String): Message<PrivateInfo> {
            if (!nickname.isNicknameValid() || !email.isEmailValid()) { return Message(Shortcut.AIF, "incorrect format of nickname or email") }

            if(nickname.exist()) { return Message(Shortcut.UNE, "user $nickname not exist") }

            try {
                val timestamp = Timestamp(Date().time)
                val user = addUser(nickname, password, ip, email, timestamp)
                val portrait = "default"
                val follower = arrayListOf<String>()
                val following = arrayListOf<String>()
                val signature = "no signature yet"
                val topic = arrayListOf<String>()
                val status = "normal"
                val school = ""
                user?.let {
                    val detail = addDetail(user.id, portrait, follower, following, signature, status, topic, school)
                    Log.register(user.id, timestamp, ip, nickname)
                    detail?.let {
                        return Message(Shortcut.OK, "ok, let's fun", PrivateInfo(user, detail))
                    }
                }

                return Message(Shortcut.OTHER, "unknown error")
            } catch (e: Exception) {
                e.printStackTrace()
                return Message(Shortcut.OTHER, "unknown error")
            }
        }

        // checked
        private fun addDetail(id: String, portrait: String, follower: ArrayList<String>, following: ArrayList<String>, signature: String, status: String, topic: ArrayList<String>, school: String): SimpleDetail.Detail? {
            try {
                val conn = MySQLConn.connection
                val ps =
                    conn.prepareStatement("insert into user_detail (id, portrait, follower, following, personal_signature, account_status, exp, log, topic, school) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                ps.setString(1, id)
                ps.setString(2, portrait)
                ps.setString(3, gson.toJson(follower))
                ps.setString(4, gson.toJson(following))
                ps.setString(5, signature)
                ps.setString(6, status)
                ps.setInt(7, 20)
                ps.setString(8, "init\n")
                ps.setString(9, gson.toJson(topic))
                ps.setString(10, school)
                ps.execute()
                ps.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return SimpleDetail.get(id)
        }

        // checked
        private fun addUser(nickname: String, password: String, ip: String, email: String, timestamp: Timestamp): SimpleUser.User? {
            val id = newId(timestamp, nickname)
            try {
                val conn = MySQLConn.connection
                val token = getToken(id, LoginPlatform.MOBILE, CONF.secretKey, timestamp, false)
                val ps =
                    conn.prepareStatement("insert into user (id, nickname, password, token, last_login_ip, last_login_time, email) VALUES (?, ?, ?, ?, ?, ?, ?)")
                ps.setString(1, id)
                ps.setString(2, nickname)
                ps.setString(3, password)
                ps.setString(4, token)
                ps.setString(5, ip)
                ps.setTimestamp(6, timestamp)
                ps.setString(7, email)
                ps.execute()
                ps.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return SimpleUser.get(id)
        }

        // checked
        fun changePortrait(req: HttpServletRequest): Message<Any> {
            val conn = MySQLConn.connection
            val multipleForm: MultipleForm = MultipleForm(req).build()

            return try {
                val fields = multipleForm.fields
                val streams = multipleForm.streams
                val id = fields["id"]
                val token = fields["token"]
                val ip = Value.getIP(req)
                val date = Date()

                if (id.isNullOrEmpty() || token.isNullOrEmpty() || ip == "0.0.0.0" || streams.isEmpty() || streams[0].field != "portrait") { return Message(Shortcut.AE, "argument mismatch") }

                when (checkToken(id, token)) {
                    Shortcut.OK -> {
                        val filename = "${id}_${Value.random()}"
                        if (multipleForm.saveSingleFile(CONF.conf.portrait, filename)) {
                            val ps = conn.prepareStatement("update user_detail set portrait = ? where id = ?")
                            ps.setString(1, filename)
                            ps.setString(2, id)
                            ps.executeUpdate()
                            ps.close()
                            Log.changePortrait(id, date, ip, true, filename)
                            Message(Shortcut.OK, "portrait changed")
                        } else {
                            Message(Shortcut.OTHER, "save file error")
                        }
                    }

                    Shortcut.TE -> {
                        Message(Shortcut.TE, "invalid token")
                    }

                    Shortcut.UNE -> {
                        Message(Shortcut.UNE, "user $id not found")
                    }

                    else -> {
                        Message(Shortcut.OTHER, "unknown error")
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
        fun changePassword(id: String, oldPassword: String, newPassword: String, ip: String): Message<Any> {
            val conn = MySQLConn.connection
            val timestamp = Timestamp(Date().time)

            try {
                var ps = conn.prepareStatement("select * from user where id = ? limit 1")
                ps.setString(1, id)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val token = getToken(id, LoginPlatform.MOBILE, "7894556", Date(), false)
                    val password = rs.getString("password")
                    if (password == oldPassword) {
                        rs.close()
                        ps.close()
                        ps = conn.prepareStatement("update user set password = ?, token = ? where id = ?")
                        ps.setString(1, newPassword)
                        ps.setString(2, token)
                        ps.setString(3, id)
                        ps.executeUpdate()
                        ps.close()
                        Log.changePassword(id, timestamp, ip, true)
                        return Message(Shortcut.OK, "change password succeed")
                    } else {
                        rs.close()
                        ps.close()
                        Log.changePassword(id, timestamp, ip, false)
                        return Message(Shortcut.UPE, "wrong password")
                    }
                } else {
                    rs.close()
                    ps.close()
                    return Message(Shortcut.UNE, "user $id has not been registered")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return Message(Shortcut.OTHER, "unknown error")
            }

        }

        // checked
        fun getPortrait(id: String?): String {
            var portrait = "default"
            if (id != null) {
                val conn = MySQLConn.connection
                try {
                    val ps = conn.prepareStatement("select portrait from user_detail where id = ? limit 1")
                    ps.setString(1, id)
                    val rs = ps.executeQuery()
                    if (rs.next()) {
                        portrait = rs.getString("portrait")
                    }
                    rs.close()
                    ps.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return portrait
        }

        // checked
        private fun newId(registerTime: Date, nickname: String?) = ("${registerTime.time}$nickname${(10..99).random()}".hashCode() and Integer.MAX_VALUE).toString()

        // checked
        fun log(id: String, log: String) {
            try {
                val conn = MySQLConn.connection
                val ps = conn.prepareStatement("update user_detail set log = concat(?, log) where id = ?")
                ps.setString(1, log)
                ps.setString(2, id)
                ps.executeUpdate()
                ps.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private val gson = CONF.gson

        enum class LoginType {
            ID, NICKNAME
        }

        enum class LoginPlatform {
            WEB, PC, MOBILE, PAD
        }
    }
}
