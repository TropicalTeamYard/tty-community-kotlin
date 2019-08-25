package util.file

import com.alibaba.fastjson.JSONObject
import net.coobird.thumbnailator.Thumbnails
import java.io.*
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletResponse

object FileUtil {
    fun readAll(file: File): String {
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

    fun writeFile2Response(path: String, resp: HttpServletResponse) {
        resp.reset()
        val inputStream = FileInputStream(path)
        val outputStream = resp.outputStream
        var len: Int
        val buffer = ByteArray(1024)
        do {
            len = inputStream.read(buffer)
            if (len == -1) {
                break
            }
            outputStream.write(buffer, 0, len)
        } while (true)
        outputStream.close()
        inputStream.close()
    }

    fun writePicture2Response(resp: HttpServletResponse, path: String, scale: Double, quality: Double) {
        resp.reset()
        val outputStream = resp.outputStream
        Thumbnails.of(path).scale(scale).outputQuality(quality).toOutputStream(outputStream)
        outputStream.close()
    }

}
