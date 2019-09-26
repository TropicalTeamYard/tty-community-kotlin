package model.orm

import java.sql.Connection


class Field(var name: String, var type: FieldType, var lenth: Int = 32, var unique: Boolean = false, var nullable: Boolean = false, var primaryKey: Boolean = false) {
    var modified: Boolean = false
    var value: Any? = null
    val ref: Any? = null

    enum class FieldType {
        CHAR, INTEGER, TEXT, TIMESTAMP, BLOB;

        val type: String
            get() {
                return when (this) {
                    CHAR -> "varchar(#char_len#)"
                    INTEGER -> "integer"
                    TEXT -> "text"
                    TIMESTAMP -> "timestamp"
                    BLOB -> "blob"
                }
            }
    }
}



class User {
    private val fields: HashSet<Field>

    private val _id = Field("_id", Field.FieldType.INTEGER, primaryKey = true)
    private val userId = Field("user_id", Field.FieldType.CHAR, unique = true)
    private val nickname = Field("nickname", Field.FieldType.CHAR, unique = true)
    private val password = Field("password", Field.FieldType.CHAR)
    private val token = Field("token", Field.FieldType.TEXT)
    private val email = Field("email", Field.FieldType.CHAR, lenth = 64)
    private val lastLoginIP = Field("last_login_ip", Field.FieldType.CHAR, lenth = 64)
    private val lastLoginTime = Field("last_login_time", Field.FieldType.TIMESTAMP)

    init {
        fields = hashSetOf(_id, userId, nickname, password, token, email, lastLoginIP, lastLoginTime)
    }

    companion object {
        private val fields = User().fields
        fun init(conn: Connection) {
            var template = """create table user("""
            for(field in fields) {
                var sql = "${field.name} ${field.type.type} "
                if (field.type == Field.FieldType.CHAR) {
                    sql = sql.replace("#char_len#", field.lenth.toString())
                }
                if(field.unique) {
                    sql = sql.plus("unique ")
                }
                if (field.primaryKey) {
                    sql = sql.plus("primary key auto_increment ")
                }
                if (field.nullable) {
                    sql = sql.plus("null")
                } else {
                    sql = sql.plus("not null")
                }
                template = template.plus(sql).plus(",")
            }

            template = template.substring(0, template.length-1).plus(");")
//            println(template)
            val s = conn.createStatement()
            s.execute(template)
        }
    }


}