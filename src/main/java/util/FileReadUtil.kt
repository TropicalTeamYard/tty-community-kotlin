package util

import com.alibaba.fastjson.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets

object FileReadUtil {
    private fun readAll(file: File): String {
        try {
            val fileReader = FileReader(file)
            val reader = InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)
            var ch: Int
            val sb = StringBuffer()
            do {
                ch = reader.read()
                if (ch == -1) {
                    break
                }
                sb.append(ch.toChar())
            } while (true)
            fileReader.close()
            reader.close()
            return sb.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }

    }

    fun readJson(file: File): JSONObject {
        val s: String = readAll(file)
        return JSONObject.parseObject(s)
    }

}
