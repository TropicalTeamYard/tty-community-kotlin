package orm

import java.util.ArrayList

interface Model {
    val fields: ArrayList<Field>
    val name: String

    fun save() {

    }

    fun get(_id: String) {
        val ps = conn.prepareStatement("select * from us")
    }

    companion object {
        val conn = Conn.connection
        private const val template = "create table {{name}}({{content}});"
        fun init(model: Model) {
            var fields = ""
            for (i in 0 until model.fields.size) {
                fields = fields.plus(model.fields[i].toSql())
                if (i != model.fields.size - 1) {
                    fields = fields.plus(",")
                }
            }
            val sql = template.replace("{{name}}", model.name).replace("{{fields}}", fields)
            println(sql)
            conn.createStatement().execute(sql)
        }
    }
}

class User: Model {
    override lateinit var fields: ArrayList<Field>
    override val name: String = "user"

    private val _id = IntegerField("_id", primaryKey = true)
    private val userId = CharField("user_id", unique = true)
    private val nickname = CharField("nickname", unique = true)
    private val password = CharField("password")
    private val token = TextField("token")
    private val email = CharField("email", length = 64)
    private val lastLoginIP = CharField("last_login_ip", length = 64)
    private val lastLoginTime = DateTimeField("last_login_time")

    init {
        fields = arrayListOf(_id, userId, nickname, password, token, email, lastLoginIP, lastLoginTime)
    }

}

