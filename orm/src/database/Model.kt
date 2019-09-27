package database

import java.util.ArrayList

class Model {
    val fields = ArrayList<Field>()
    abstract val name: String

    companion object {
        fun init(model: Model, conn: Connection) {
            for (i in 0 until model.fields.size) {

            }
        }
    }
}