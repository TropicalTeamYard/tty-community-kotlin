package orm

interface Field {
    val name: String
    val unique: Boolean
    val nullable: Boolean
    val primaryKey: Boolean
    var modified: Boolean
    var value: Any?
    fun sqlType(): String
    val type: Type
    fun toSql(): String {
        var sql = "$name ${sqlType()} "
        if(unique) { sql = sql.plus("unique ") }
        if(primaryKey) { sql = sql.plus("primary key auto_increment ") }
        sql = if (nullable) { sql.plus("null") } else { sql.plus("not null") }
        return sql
    }
}

class CharField(
        override val name: String,
        private val length: Int = 32,
        override val unique: Boolean = false,
        override val nullable: Boolean = false,
        override val primaryKey: Boolean = false
): Field {
    override fun sqlType(): String = "varchar($length)"

    override var modified: Boolean = false
    override var value: Any? = null
        set(value) {
            if (value != field && value is String) {
                field = value
                modified = true
            }
        }
    override val type: Type = Type.CHAR
}

class IntegerField(
        override val name: String,
        override val unique: Boolean = false,
        override val nullable: Boolean = false,
        override val primaryKey: Boolean = false
): Field {
    override fun sqlType(): String = "integer"

    override var modified: Boolean = false
    override var value: Any? = null
        set(value) {
            if (value != field) {
                field = value
                modified = true
            }
        }
    override val type: Type = Type.INTEGER
}

class DateTimeField(
        override val name: String,
        override val unique: Boolean = false,
        override val nullable: Boolean = false,
        override val primaryKey: Boolean = false
): Field {
    override fun sqlType(): String = "timestamp"

    override var modified: Boolean = false
    override var value: Any? = null
        set(value) {
            if (value != field) {
                field = value
                modified = true
            }
        }
    override val type: Type = Type.TIMESTAMP
}

class TextField(
        override val name: String,
        override val unique: Boolean = false,
        override val nullable: Boolean = false,
        override val primaryKey: Boolean = false
): Field {
    override fun sqlType(): String = "text"

    override var modified: Boolean = false
    override var value: Any? = null
        set(value) {
            if (value != field) {
                field = value
                modified = true
            }
        }
    override val type: Type = Type.TEXT
}

class BlobField(
        override val name: String,
        override val unique: Boolean = false,
        override val nullable: Boolean = false,
        override val primaryKey: Boolean = false
): Field {
    override fun sqlType(): String = "blob"

    override var modified: Boolean = false
    override var value: Any? = null
        set(value) {
            if (value != field) {
                field = value
                modified = true
            }
        }
    override val type: Type = Type.BLOB
}

enum class Type {
    CHAR, INTEGER, TEXT, TIMESTAMP, BLOB;
}
