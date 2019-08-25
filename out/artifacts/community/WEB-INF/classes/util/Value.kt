package util

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import enums.Shortcut
import util.file.FileReadUtil
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Blob
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.collections.HashMap
import kotlin.experimental.and

object Value {
    const val DEFAULT_NICKNAME = "[RESET]"

    fun Blob.string(): String {
        return try {
            val string: String
            val inputStream = binaryStream
            val byteArrayInputStream = inputStream as ByteArrayInputStream
            val byteData = ByteArray(byteArrayInputStream.available()) //byteArrayInputStream.available()返回此输入流的字节数
            byteArrayInputStream.read(byteData, 0, byteData.size) //将输入流中的内容读到指定的数组
            string = String(byteData, StandardCharsets.UTF_8) //再转为String，并使用指定的编码方式
            inputStream.close()
            string
        } catch (e: Exception) {
            ""
        }
    }

    fun String?.value(default: Int = 0): Int {
        try { this?.let { return it.toInt() } } catch (e: Exception) { }
        return default
    }

    fun getTime(date: Date): String {
        val time: String
        val sdf = SimpleDateFormat(CONF.dateFormat)
        time = sdf.format(date)
        return time
    }

    fun getTime(s: String?): Date? {
        if (s == null) {
            return null
        }
        return try {
            val sdf = SimpleDateFormat(CONF.dateFormat)
            sdf.parse(s)
        } catch (e: ParseException) {
            null
        }
    }

    fun getMD5(input: String): String {
        val messageDigest = MessageDigest.getInstance("MD5")
        val inputByteArray = input.toByteArray()
        messageDigest.update(inputByteArray)
        val resultByteArray = messageDigest.digest()
        return byteArrayToHex(resultByteArray)

    }

    private fun byteArrayToHex(byteArray: ByteArray): String {
        val hexDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
        val resultCharArray = CharArray(byteArray.size * 2)
        var index = 0

        for (b in byteArray) {
            resultCharArray[index++] = hexDigits[b.toInt().ushr(4) and 0xf]
            resultCharArray[index++] = hexDigits[(b and 0xf).toInt()]
        }

        return String(resultCharArray)
    }

    fun json(shortcut: Shortcut, msg: String, data: HashMap<String, String>): String {
        val map = JSONObject()
        map["shortcut"] = shortcut.name
        map["msg"] = msg
        map["data"] = JSONObject(data as Map<String, Any>?)
        return map.toJSONString()
    }

    fun json(shortcut: Shortcut, msg: String, list: List<Any>): String {
        val map = JSONObject()
        map["shortcut"] = shortcut.name
        map["msg"] = msg
        map["data"] = JSONArray(list)
        return map.toJSONString()
    }

    fun json(shortcut: Shortcut, msg: String): String {
        val map = JSONObject()
        map["shortcut"] = shortcut.name
        map["msg"] = msg
        return map.toJSONString()
    }

    fun htmlTemplate(): String = FileReadUtil.readAll(File("${CONF.root}/html/template.html"))

    fun markdownAirCss(): String = FileReadUtil.readAll(File("${CONF.root}/css/markdown-air.css"))

    fun random() = ("${Date().time}${(10000000..99999999).random()}".hashCode() and Integer.MAX_VALUE).toString()

    fun Map<String, Array<String>>.fields(): HashMap<String, String> {
        val fields = HashMap<String, String>()
        for (item in this) {
            val key = item.key
            val value = item.value[0]
            if (value.isEmpty()) {
                continue
            }
            fields[key] = value
        }

        return fields
    }

    fun getIP(request: HttpServletRequest): String {
        var ip: String? = request.getHeader("x-forwarded-for")
        if (ip != null && ip.isNotEmpty() && !"unknown".equals(ip, ignoreCase = true)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.contains(",")) {
                ip = ip.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            }
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_CLIENT_IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("X-Real-IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }
        return ip ?: "0.0.0.0"
    }

}