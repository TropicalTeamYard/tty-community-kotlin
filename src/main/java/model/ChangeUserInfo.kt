package model

import com.alibaba.fastjson.JSONObject
import util.log.Log
import util.conn.MySQLConn
import util.enums.Shortcut
import util.Value
import java.sql.SQLException
import java.util.*
import kotlin.collections.HashMap

@Suppress("ReplaceWithEnumMap")
class ChangeUserInfo(var id: String, var token: String, var ip: String) {
    var changedItem: HashMap<UserInfoType, String> = HashMap()
    private val date = Date()
    var succeedItem: HashMap<String, String> = HashMap()
    fun submit(): String{
        val conn = MySQLConn.connection
        try {
            var ps = conn.prepareStatement("select  * from user where id = ? limit 1")
            ps.setString(1, id)
            val rs = ps.executeQuery()
            if(rs.next()){
                if(token == Value.getMD5(rs.getString("token"))){
                    val nicknameBefore = rs.getString("nickname")
                    val emailBefore = rs.getString("email")
                    rs.close()
                    ps.close()

                    for(e in changedItem){
                        when(e.key){
                            UserInfoType.Email -> {
                                ps = conn.prepareStatement("update user set email = ? where id = ?")
                                ps.setString(1, e.value)
                                ps.setString(2, id)
                                ps.executeUpdate()
                                ps.close()
                                Log.changeUserInfo(id, date, ip, true, emailBefore, e.value, UserInfoType.Email)
                                succeedItem["email"] = e.value
                            }
                            UserInfoType.Nickname -> {
                                val isNicknameRegistered = Register.checkNickname(e.value)
                                if (!isNicknameRegistered){
                                    ps = conn.prepareStatement("update user set nickname = ? where id = ?")
                                    ps.setString(1, e.value)
                                    ps.setString(2, id)
                                    ps.executeUpdate()
                                    ps.close()
                                    Log.changeUserInfo(id, date, ip, true, nicknameBefore, e.value, UserInfoType.Nickname)
                                    succeedItem["nickname"] = e.value
                                } else {
                                    Log.changeUserInfo(id, date, ip, false)
                                }

                            }
                            UserInfoType.Default -> {}
                        }
                    }

                    return json(Shortcut.OK, "change user info succeed.", succeedItem)

                } else {
                    rs.close()
                    ps.close()
                    Log.changeUserInfo(id, date, ip, false)
                    return json(Shortcut.TE, "invalid token")
                }

            } else {
                rs.close()
                ps.close()
                Log.changeUserInfo(id, date, ip, false)
                return json(Shortcut.UNE, "user $id has not been registered.")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return json(Shortcut.OTHER, "SQL ERROR")
        }
    }

    companion object {
        fun json(shortcut: Shortcut, msg: String, data: HashMap<String, String> ?= null): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            if(data!=null){
                map["data"] = JSONObject(data as Map<String, Any>?)
            }
            return map.toJSONString()
        }
    }
}

class ChangePortrait(val id: String, private val token: String, var ip: String) {
    private val date = Date()
    fun submit(): String{
        TODO()
    }
}

class ChangePassword(private var id: String, private var oldPassword: String, private var newPassword: String, var ip: String) {

    private val conn = MySQLConn.connection
    private val date = Date()

    fun submit(): String{
        try {
            var ps = conn.prepareStatement("select * from user where id = ? limit 1")
            ps.setString(1, id)
            val rs = ps.executeQuery()
            if (rs.next()){
                val password = rs.getString("password")
                if (password == oldPassword){
                    rs.close()
                    ps.close()
                    ps = conn.prepareStatement("update user set password = ? where id = ?")
                    ps.setString(1, newPassword)
                    ps.setString(2, id)
                    ps.executeUpdate()
                    ps.close()

                    Log.changePassword(id, date, ip, true)
                    return json(Shortcut.OK, "change password succeed.")
                } else {
                    rs.close()
                    ps.close()
                    Log.changePassword(id, date, ip, false)
                    return json(Shortcut.UPE, "wrong password.")
                }
            } else {
                rs.close()
                ps.close()
                return json(Shortcut.UNE, "user $id has not been registered.")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return json(Shortcut.OTHER, "SQL ERROR")
        }
    }

    companion object {
        private fun json(shortcut: Shortcut, msg: String, data: HashMap<String, String>?=null): String {
            val map = JSONObject()
            map["shortcut"] = shortcut.name
            map["msg"] = msg
            if(data!=null){
                map["data"] = JSONObject(data as Map<String, Any>?)
            }
            return map.toJSONString()
        }
    }

}

class ChangeDetailInfo(private var id: String, private var token: String, private var ip: String) {
    var changedItem = HashMap<String, String>()
    var successItem = HashMap<String, String>()
    fun submit(): String{
        val conn = MySQLConn.connection
        val date = Date()
        try {
            var ps = conn.prepareStatement("select token from user where id = ? limit 1")
            ps.setString(1, id)
            var rs = ps.executeQuery()
            if (rs.next() && Value.getMD5(rs.getString("token")) == token){
                rs.close()
                ps.close()
                ps = conn.prepareStatement("select * from user_detail where id = ? limit 1")
                ps.setString(1, id)
                rs = ps.executeQuery()
                rs.next()
                for (item in changedItem) {
                    if(DetailInfoType.items.contains(item.key)) {
                        val valueBefore = rs.getString(item.key)
                        val ps1 = conn.prepareStatement("update user_detail set ${item.key} = ? where id = ?")
                        ps1.setString(1, item.value)
                        ps1.setString(2, id)
                        ps1.executeUpdate()
                        ps1.close()
                        Log.changeUserDetailInfo(id, date, ip, true, valueBefore, item.value, item.key)
                        successItem[item.key] = item.value
                    } else {
                        Log.changeUserDetailInfo(id, date, ip, false, target = item.key)
                    }
                }

                rs.close()
                ps.close()
                return ChangeUserInfo.json(Shortcut.OK, "change user info succeed", successItem)

            } else {
                rs.close()
                ps.close()
                return ChangeUserInfo.json(Shortcut.TE, "invalid token.")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            return ChangeUserInfo.json(Shortcut.OTHER, "SQL ERROR")
        }
    }
}

enum class UserInfoType{
    Email, Nickname, Default
}

object DetailInfoType {
    val items = arrayOf("personal_signature", "portrait")
}