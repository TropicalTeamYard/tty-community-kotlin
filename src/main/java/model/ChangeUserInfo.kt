package model

import com.alibaba.fastjson.JSONObject
import util.Log
import util.MySQLConn
import util.Shortcut
import util.StringUtil
import java.sql.SQLException
import java.util.*
import kotlin.collections.HashMap

class ChangeUserInfo(var id: String, var token: String, var ip: String) {
    var changedItem: HashMap<UserInfoType, String> = HashMap()
    private val date = Date()
    var succeedItem: HashMap<String, String> = HashMap()
    fun submit(): String{
        val conn = MySQLConn.mySQLConnection
        try {
            var ps = conn.prepareStatement("select  * from user where id = ? limit 1")
            ps.setString(1, id)
            val rs = ps.executeQuery()
            if(rs.next()){
                if(token == StringUtil.getMd5(rs.getString("token"))){
                    val nicknameBefore = rs.getString("nickname")
                    val emailBefore = rs.getString("email")
                    rs.close()
                    ps.close()

                    for(e in changedItem){
                        when(e.key){
                            UserInfoType.Email -> {
                                ps = conn.prepareStatement("update user set email = ?, log = concat(?, log) where id = ?")
                                ps.setString(1, e.value)
                                ps.setString(2, Log.changeUserInfo(date, ip, true, emailBefore, e.value, UserInfoType.Email))
                                ps.setString(3, id)
                                ps.executeUpdate()
                                ps.close()
                                succeedItem["email"] = e.value
                            }
                            UserInfoType.Nickname -> {
                                val isNicknameRegistered = Register.checkNickname(e.value)
                                if (!isNicknameRegistered){
                                    ps = conn.prepareStatement("update user set nickname = ?, log = concat(?, log) where id = ?")
                                    ps.setString(1, e.value)
                                    ps.setString(2, Log.changeUserInfo(date, ip, true, nicknameBefore, e.value, UserInfoType.Nickname))
                                    ps.setString(3, id)
                                    ps.executeUpdate()
                                    ps.close()
                                    succeedItem["nickname"] = e.value
                                } else {
                                    ps = conn.prepareStatement("update user set log = concat(?, log) where id = ?")
                                    ps.setString(1, Log.changeUserInfo(date, ip, false))
                                    ps.setString(2, id)
                                    ps.executeUpdate()
                                    ps.close()
                                }

                            }
                            UserInfoType.Default -> {}
                        }
                    }

                    return json(Shortcut.OK, "change user info succeed.", succeedItem)

                } else {
                    rs.close()
                    ps.close()

                    ps = conn.prepareStatement("update user set log = concat(?, log) where id = ?")
                    ps.setString(1, Log.changeUserInfo(date, ip, false))
                    ps.setString(2, id)
                    ps.executeUpdate()
                    ps.close()
                    return json(Shortcut.UPE, "invalid token")
                }

            } else {
                rs.close()
                ps.close()

                ps = conn.prepareStatement("update user set log = concat(?, log) where id = ?")
                ps.setString(1, Log.changeUserInfo(date, ip, false))
                ps.setString(2, id)
                ps.executeUpdate()
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

enum class UserInfoType{
    Email, Nickname, Default
}