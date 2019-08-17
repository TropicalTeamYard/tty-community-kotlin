package model.log

import model.Blog
import model.Topic
import model.User
import model.User.Companion.LoginPlatform
import model.User.Companion.LoginType
import util.CONF
import util.Value
import java.util.*

object Log {
    private val gson = CONF.gson

    fun register(id: String, date: Date, ip: String, nickname: String) {
        val log = "user::register::nickname=$nickname::time=${Value.getTime(date)}::ip=$ip\n"
        User.log(id, log)
    }

    fun login(id: String, date: Date, ip: String, loginType: LoginType, platform: LoginPlatform, status: Boolean) {
        val log = "user::login::" + when (status) {
            true -> "success"
            false -> "failed"
        } + "::time=${Value.getTime(date)}::login_type=${loginType.name}::platform=${platform.name}::ip=$ip\n"
        User.log(id, log)
    }

    fun autoLogin(id: String, date: Date, ip: String, platform: LoginPlatform, status: Boolean) {
        val log =
            "user::auto_login::" + when (status) {
                true -> "success"
                false -> "failed"
            } + "::time=${Value.getTime(date)}::platform=${platform.name}::ip=$ip\n"
        User.log(id, log)
    }

    fun changeUserInfo(id: String, date: Date, ip: String, before: User.PrivateInfo?, after: User.PrivateInfo?) {
        val log = "user::change_info::time=${Value.getTime(date)}::ip=$ip::before=${gson.toJson(
            before,
            User.PrivateInfo::class.java
        )}::after=${gson.toJson(after, User.PrivateInfo::class.java)}\n"
        User.log(id, log)
    }

    fun changePortrait(id: String, date: Date, ip: String, status: Boolean, after: String? = null) {
        val log = "user::change_portrait::${
        when (status) {
            true -> "success::time=${Value.getTime(date)}::ip=$ip::after=$after"
            false -> "failed::time=${Value.getTime(date)}::ip=$ip"
        }
        }\n"
        User.log(id, log)
    }

    fun changePassword(id: String, date: Date, ip: String, status: Boolean) {
        val log = "user::change_password::${
        when (status) {
            true -> "success"
            false -> "failed"
        }
        }::ip=$ip::date=${Value.getTime(date)}\n"
        User.log(id, log)
    }

    fun createBlog(id: String, date: Date, ip: String, status: Boolean, blogId: String? = null) {
        val user = "blog::create::" +
                when (status) {
                    true -> "success::ip=$ip::date=${Value.getTime(date)}::blogId=$blogId"
                    false -> "failed::ip=$ip::date=${Value.getTime(date)}"
                } + "\n"
        User.log(id, user)
        if (status && blogId != null) {
            val blog = "blog::create::success::date=${Value.getTime(date)}::ip=$ip::author=$id\n"
            Blog.log(blogId, blog)
        }
    }

    fun editBlog() {
        // TODO
    }

    fun createTopic(
        id: String,
        date: Date,
        ip: String,
        status: Boolean,
        topicId: String? = null,
        name: String? = null
    ) {
        val user = "topic::create::${
        when (status) {
            true -> "success::ip=$ip::date=${Value.getTime(date)}::topicId=$topicId::name=$name"
            false -> "failed::ip=$ip::date=${Value.getTime(date)}"
        }
        }\n"

        User.log(id, user)

        if (status && topicId != null && name != null) {
            val blog =
                "topic::create::success::date=${Value.getTime(date)}::ip=$ip::admin=$id::topicId=$topicId::name=$name\n"
            Topic.log(topicId, blog)
        }
    }

}


