package model

import com.google.gson.Gson
import exception.Shortcut
import exception.ShortcutThrowable
import util.CONF
import util.Value
import util.conn.MySQLConn
import util.log.Log
import util.parse.IP
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
        val shortcut = checkToken(id, token)
        val conn = MySQLConn.connection
        private val items = ArrayList<Item>()
        var date = Date()
        private var before: PrivateInfo? = null
        private var after: PrivateInfo? = null

        init {
            try {
                before = PrivateInfo.get(id, token)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun add(item: Item) {
            items.add(item)
        }

        fun result(): ArrayList<Item> {
            try {
                after = PrivateInfo.get(id, token)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Log.changeUserInfo(id, date, ip, before, after)

            return items
        }

        data class Item(val key: String, val value: String, val status: Shortcut)

        fun nickname(value: String): Item {
            val key = "nickname"
            if (shortcut != Shortcut.OK) {
                return Item(key, value, shortcut)
            }
            if (!value.isNicknameValid()) {
                return Item(key, value, Shortcut.AIF)
            }
            if (value.exist()) {
                return Item(key, value, Shortcut.UR)
            }

            return try {
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
            } catch (e: SQLException) {
                e.printStackTrace()
                Item(key, value, Shortcut.OTHER)
            }
        }

        fun email(value: String): Item {
            val key = "email"
            if (shortcut != Shortcut.OK) {
                return Item(key, value, shortcut)
            }
            if (!value.isEmailValid()) {
                return Item(key, value, Shortcut.AIF)
            }
            return try {
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
            } catch (e: SQLException) {
                e.printStackTrace()
                Item(key, value, Shortcut.OTHER)
            }
        }

        fun signature(signature: String): Item {
            val key = "signature"
            if (shortcut != Shortcut.OK) {
                return Item(key, signature, shortcut)
            }
            return try {
                val ps = conn.prepareStatement("update user_detail set personal_signature = ? where id = ?")
                ps.setString(1, signature)
                ps.setString(2, id)
                val effect = ps.executeUpdate()
                ps.close()
                if (effect > 0) {
                    Item(key, signature, Shortcut.OK)
                } else {
                    Item(key, signature, Shortcut.UNE)
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                Item(key, signature, Shortcut.OTHER)
            }
        }

        fun school(value: String): Item {
            val key = "school"
            if (shortcut != Shortcut.OK) {
                return Item(key, value, shortcut)
            }
            if (!value.isSchoolValid()) {
                return Item(key, value, Shortcut.AIF)
            }
            return try {
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
            } catch (e: SQLException) {
                e.printStackTrace()
                Item(key, value, Shortcut.OTHER)
            }
        }

        @Deprecated("use ChangePortrait")
        fun portrait(value: String): Item {
            val key = "portrait"
            if (shortcut != Shortcut.OK) {
                return Item(key, value, shortcut)
            }
            return try {
                val ps = conn.prepareStatement("update user_detail set portrait = ? where id = ?")
                ps.setString(1, value)
                ps.setString(2, id)
                val effect = ps.executeUpdate()
                ps.close()
                if (effect > 0) {
                    Item(key, value, Shortcut.OK)
                } else {
                    Item(key, value, Shortcut.UNE)
                }
            } catch (e: SQLException) {
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
        override val userGroup: String,
        override val exp: String,
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
            fun get(id: String, token: String): PrivateInfo {
                when (checkToken(id, token)) {
                    Shortcut.OK -> {
                        val user = SimpleUser.get(id) ?: throw ShortcutThrowable.UNE()

                        val detail = SimpleDetail.get(id) ?: throw throw ShortcutThrowable.UNE()

                        return PrivateInfo(user, detail)
                    }

                    Shortcut.TE -> {
                        throw ShortcutThrowable.TE()
                    }

                    Shortcut.UNE -> {
                        throw ShortcutThrowable.UNE()
                    }

                    else -> {
                        throw ShortcutThrowable.OTHER("error when checking the token")
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
                val conn = MySQLConn.connection
                val ps = conn.prepareStatement("select nickname, email from user where id = ? limit 1")
                ps.setString(1, id)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val nickname = rs.getString("nickname")
                    val email = rs.getString("email")
                    rs.close()
                    ps.close()
                    return User(id, nickname, email)
                } else {
                    rs.close()
                    ps.close()
                }

                return null
            }
        }
    }

    interface SimpleDetail {
        val portrait: String
        val signature: String
        val userGroup: String
        val exp: String
        val school: String

        class Detail(
            override val portrait: String,
            override val signature: String,
            override val userGroup: String,
            override val exp: String,
            override val school: String
        ) : SimpleDetail

        companion object {
            fun get(id: String): Detail? {
                val conn = MySQLConn.connection
                val ps =
                    conn.prepareStatement("select portrait, personal_signature, user_group, exp, school from user_detail where id = ? limit 1")
                ps.setString(1, id)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val portrait = rs.getString("portrait")
                    val signature = rs.getString("personal_signature")
                    val userGroup = rs.getString("user_group")
                    val exp = rs.getInt("exp").toString()
                    val school = rs.getString("school")
                    rs.close()
                    ps.close()
                    return Detail(portrait, signature, userGroup, exp, school)
                } else {
                    rs.close()
                    ps.close()
                }
                return null
            }
        }
    }

    companion object {
        fun getToken(id: String, platform: LoginPlatform, secret: String, time: Date, status: Boolean): String {
            return "$id::${platform.name}::$secret::${Value.getTime(time)}::$status"
        }

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

        fun getIdByNickname(nickname: String): String? {
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

        fun String.isNicknameValid(): Boolean {
            return Pattern.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5]+$", this)
        }

        fun String.isEmailValid(): Boolean {
            return Pattern.matches("^[A-Za-z0-9\\u4e00-\\u9fa5]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+\$", this)
        }

        fun String.isSchoolValid(): Boolean {
            // todo check the school name valid
            return this.isNotEmpty()
        }

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
            } catch (e: SQLException) {
                e.printStackTrace()
                return Shortcut.OTHER
            }
        }

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

            } catch (e: SQLException) {
                e.printStackTrace()
                return Shortcut.OTHER
            }
        }

        private fun updateLogin(id: String, ip: String, timestamp: Timestamp): Int {
            val conn = MySQLConn.connection
            val ps = conn.prepareStatement("update user set last_login_ip = ?, last_login_time = ? where id = ?")
            ps.setString(1, ip)
            ps.setTimestamp(2, timestamp)
            ps.setString(3, id)
            val effect = ps.executeUpdate()
            ps.close()
            return effect
        }

        private fun updateToken(id: String, token: String): Int {
            val conn = MySQLConn.connection
            val ps = conn.prepareStatement("update user set token = ? where id = ?")
            ps.setString(1, token)
            ps.setString(2, id)
            val effect = ps.executeUpdate()
            ps.close()
            return effect
        }

        fun autoLogin(ip: String, id: String, token: String, platform: LoginPlatform): ShortcutThrowable {
            try {
                val timestamp = Timestamp(Date().time)
                when (checkToken(id, token)) {
                    Shortcut.OK -> {
                        updateLogin(id, ip, timestamp)
                        val simple = SimpleUser.get(id) ?: return ShortcutThrowable.UNE()
                        Log.autoLogin(id, timestamp, ip, platform, true)
                        return ShortcutThrowable.OK("ok, let's fun", simple)
                    }

                    Shortcut.TE -> {
                        Log.autoLogin(id, timestamp, ip, platform, false)
                        return ShortcutThrowable.TE()
                    }

                    Shortcut.UNE -> {
                        return ShortcutThrowable.UNE()
                    }

                    else -> {
                        return ShortcutThrowable.OTHER("error when checking the token")
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                return ShortcutThrowable.OTHER("SQL ERROR")
            }

        }

        fun login(
            account: String,
            password: String,
            type: LoginType,
            platform: LoginPlatform,
            ip: String
        ): ShortcutThrowable {
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
                            return ShortcutThrowable.OK("ok, let's fun", result)
                        }
                    }
                    return ShortcutThrowable.UNE()
                }
                Shortcut.UNE -> {
                    return ShortcutThrowable.UNE()
                }
                Shortcut.UPE -> {
                    id?.let {
                        Log.login(it, time, ip, type, platform, false)
                    }
                    return ShortcutThrowable.UPE()
                }
                else -> {
                    return ShortcutThrowable.OTHER("error when checking the password")
                }
            }
        }

        fun register(nickname: String, ip: String, email: String, password: String): ShortcutThrowable {
            if (!nickname.isNicknameValid() || !email.isEmailValid()) {
                return ShortcutThrowable.AIF("incorrect format of nickname or email")
            }

            if(nickname.exist()) {
                return ShortcutThrowable.UR()
            }

            try {
                val timestamp = Timestamp(Date().time)
                val id = addUser(nickname, password, ip, email, timestamp)
                val portrait = "default"
                val follower = arrayListOf<String>()
                val following = arrayListOf<String>()
                val signature = "no signature yet"
                val topic = arrayListOf<String>()
                val status = "normal"
                val school = ""
                addDetail(id, portrait, follower, following, signature, status, topic, school)
                val user = SimpleUser.get(id) ?: return ShortcutThrowable.UNE()
                val detail = SimpleDetail.get(id) ?: return ShortcutThrowable.UNE()
                Log.register(id, timestamp, ip, nickname)
                return ShortcutThrowable.OK("ok, let's fun", PrivateInfo(user, detail))
            } catch (e: SQLException) {
                e.printStackTrace()
                return ShortcutThrowable.OTHER("SQL ERROR")
            }
        }

        private fun addDetail(
            id: String,
            portrait: String,
            follower: ArrayList<String>,
            following: ArrayList<String>,
            signature: String,
            status: String,
            topic: ArrayList<String>,
            school: String
        ) {
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
        }

        private fun addUser(
            nickname: String,
            password: String,
            ip: String,
            email: String,
            timestamp: Timestamp
        ): String {
            val conn = MySQLConn.connection
            val id = newId(timestamp, nickname)
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
            return id
        }

        @Throws(ShortcutThrowable::class)
        fun changePortrait(req: HttpServletRequest): Boolean {
            val conn = MySQLConn.connection
            val multipleForm: MultipleForm = MultipleForm(req).build()
            try {
                val fields = multipleForm.fields
                val streams = multipleForm.streams
                val id = fields["id"]
                val token = fields["token"]
                val ip = IP.getIPAddr(req)
                val date = Date()

                if (id.isNullOrEmpty() || token.isNullOrEmpty() || ip == "0.0.0.0") {
                    throw ShortcutThrowable.AE()
                }

                if (streams.isEmpty() || streams[0].field != "portrait") {
                    throw ShortcutThrowable.AE()
                }

                when (User.checkToken(id, token)) {
                    Shortcut.OK -> {
                        val name = "${id}_${Value.random()}"
                        try {
                            if (multipleForm.saveSingleFile(CONF.conf.portrait, name)) {
                                val ps = conn.prepareStatement("update user_detail set portrait = ? where id = ?")
                                ps.setString(1, name)
                                ps.setString(2, id)
                                ps.executeUpdate()
                                ps.close()
                                Log.changePortrait(id, date, ip, true, name)
                                ShortcutThrowable.OK("portrait changed")
                                return true
                            } else {
                                throw ShortcutThrowable.OTHER("save file error")
                            }
                        } catch (e: SQLException) {
                            e.printStackTrace()
                            throw ShortcutThrowable.OTHER("SQL ERROR")
                        }
                    }

                    Shortcut.UNE -> {
                        throw ShortcutThrowable.UNE()
                    }
                    Shortcut.TE -> {
                        Log.changePortrait(id, date, ip, false)
                        throw ShortcutThrowable.TE()
                    }
                    else -> {
                        throw ShortcutThrowable.OTHER("error when checking the user")
                    }
                }
            } catch (e: ShortcutThrowable) {
                throw e
            } finally {
                multipleForm.close()
            }

        }

        fun changePassword(id: String, oldPassword: String, newPassword: String, ip: String): ShortcutThrowable {
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
                        return ShortcutThrowable.OK("change password succeed")
                    } else {
                        rs.close()
                        ps.close()
                        Log.changePassword(id, timestamp, ip, false)
                        return ShortcutThrowable.UPE("wrong password")
                    }
                } else {
                    rs.close()
                    ps.close()
                    return ShortcutThrowable.UNE("user $id has not been registered")
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                return ShortcutThrowable.OTHER("SQL ERROR")
            }

        }

        fun newId(registerTime: Date, nickname: String?) =
            ("${registerTime.time}$nickname${(10..99).random()}".hashCode() and Integer.MAX_VALUE).toString()

        fun log(id: String, log: String) {
            val conn = MySQLConn.connection
            val ps = conn.prepareStatement("update user_detail set log = concat(?, log) where id = ?")
            ps.setString(1, log)
            ps.setString(2, id)
            ps.executeUpdate()
            ps.close()
        }

        val gson = Gson()

        enum class LoginType {
            ID, NICKNAME
        }

        enum class LoginPlatform {
            WEB, PC, MOBILE, PAD
        }
    }
}
