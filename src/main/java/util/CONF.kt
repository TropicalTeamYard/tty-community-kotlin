package util

import util.enums.MODE
import util.file.FileReadUtil
import java.io.File

class CONF(
    path: String,
    var portrait: String,
    var blog: String,
    var file: String,
    var server: String,
    var user: String,
    val password: String
) {

    init {
        portrait = "$path/$portrait"
        blog = "$path/$blog"
        file = "$path/$file"
    }

    companion object {
        private val mode = MODE.DEBUG

        val root: String
            get() {
                val url = CONF::class.java.classLoader.getResource("./")
                return File(File(url!!.path).parent).parent
            }

        val conf: CONF
            get() {
                val conf = when(mode) {
                    MODE.DEBUG -> {
                        File("$root/conf/path_debug.json")
                    }
                    MODE.RELEASE -> {
                        File("$root/conf/path_release.json")
                    }
                }
                val json = FileReadUtil.readJson(conf)
                val root = json.getString("root")
                val portrait = "portrait"
                val blog = "blog"
                val file = "file"
                val server = json.getString("server")
                val user = json.getString("user")
                val password = json.getString("password")
                return CONF(root, portrait, blog, file, server, user, password)
            }
    }
}