package model.orm

import java.sql.Connection


class Field(var name: String, var type: FieldType, var lenth: Int = 32, var unique: Boolean = false, var nullable: Boolean = false, var primaryKey: Boolean = false) {
    var modified: Boolean = false
    var value: Any? = null
    val ref: Any? = null

    enum class FieldType {
        CHAR, INTEGER, TEXT, TIMESTAMP, BLOB, REF
    }
}



class User {
    private val fields: HashSet<Field>

    private val userId = Field("user_id", Field.FieldType.CHAR, unique = true, primaryKey = true)
    private val nickname = Field("nickname", Field.FieldType.CHAR, unique = true)
    private val password = Field("password", Field.FieldType.CHAR)
    private val token = Field("token", Field.FieldType.TEXT)
    private val email = Field("email", Field.FieldType.CHAR, lenth = 64)
    private val lastLoginIP = Field("last_login_ip", Field.FieldType.CHAR, lenth = 64)
    private val lastLoginTime = Field("last_login_time", Field.FieldType.TIMESTAMP)

    init {
        fields = hashSetOf(userId, nickname, password, token, email, lastLoginIP, lastLoginTime)
    }

    companion object {
        fun init(conn: Connection) {
            val sql = "create table user(" +
                    "" +
                    ");"

        }
    }
}