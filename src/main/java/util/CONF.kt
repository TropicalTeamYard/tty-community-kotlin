package util

import util.file.FileReadUtil
import java.io.File

class CONF(
    path: String,
    var portrait: String,
    var blog: String,
    var topic: String,
    var file: String,
    var server: String,
    var user: String,
    val password: String
) {

    init {
        portrait = "$path/$portrait"
        blog = "$path/$blog"
        topic = "$path/$topic"
        file = "$path/$file"
    }

    enum class MODE {
        DEBUG, RELEASE
    }

    companion object {
        private val mode = MODE.DEBUG

        

        const val secretKey = "123456"

        val root: String
            get() {
                val url = CONF::class.java.classLoader.getResource("./")
                return File(File(url!!.path).parent).parent
            }

        val conf: CONF
            get() {
                val conf = when (mode) {
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
                val topic = "topic"
                val file = "file"
                val server = json.getString("server")
                val user = json.getString("user")
                val password = json.getString("password")
                return CONF(root, portrait, blog, topic, file, server, user, password)
            }
    }
}

