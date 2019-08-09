package util

import com.alibaba.fastjson.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.sql.Blob
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.experimental.and
import java.io.IOException
import java.io.FileInputStream
import java.text.ParseException


object StringUtil {

    val time: String
        get() {
            return getTime(Date())
        }

    @Throws(SQLException::class, IOException::class)
    fun blob2String(blob: Blob): String {
        val reString: String
        val `is` = blob.binaryStream
        val byteArrayInputStream = `is` as ByteArrayInputStream
        val byteData = ByteArray(byteArrayInputStream.available()) //byteArrayInputStream.available()返回此输入流的字节数
        byteArrayInputStream.read(byteData, 0, byteData.size) //将输入流中的内容读到指定的数组
        reString = String(byteData, StandardCharsets.UTF_8) //再转为String，并使用指定的编码方式
        `is`.close()
        return reString
    }

    fun getTime(date: Date): String {
        val time: String
        val sdf = SimpleDateFormat("yyyy/MM/dd-HH:mm:ss")
        time = sdf.format(date)
        return time
    }

    fun getTime(s: String?): Date? {
        if (s == null) {
            return null
        }

        return try {
            val sdf = SimpleDateFormat("yyyy/MM/dd-HH:mm:ss")
            sdf.parse(s)
        } catch (e: ParseException){
            null
        }

    }

    fun getMD5(input: String): String? {
        return try {
            //拿到一个MD5转换器（如果想要SHA1加密参数换成"SHA1"）
            val messageDigest = MessageDigest.getInstance("MD5")
            //输入的字符串转换成字节数组
            val inputByteArray = input.toByteArray()
            //inputByteArray是输入字符串转换得到的字节数组
            messageDigest.update(inputByteArray)
            //转换并返回结果，也是字节数组，包含16个元素
            val resultByteArray = messageDigest.digest()
            //字符数组转换成字符串返回
            byteArrayToHex(resultByteArray)

        } catch (e: NoSuchAlgorithmException) {
            null
        }

    }

    private fun byteArrayToHex(byteArray: ByteArray): String {
        //首先初始化一个字符数组，用来存放每个16进制字符
        val hexDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
        //new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符）
        val resultCharArray = CharArray(byteArray.size * 2)
        //遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
        var index = 0
        for (b in byteArray) {
            resultCharArray[index++] = hexDigits[b.toInt().ushr(4) and 0xf]
            resultCharArray[index++] = hexDigits[(b and 0xf).toInt() ]
        }

        //字符数组组合成字符串返回
        return String(resultCharArray)
    }

    fun json(shortcut: Shortcut, msg: String, data: HashMap<String, String>?=null): String {
        val map = JSONObject()
        map["shortcut"] = shortcut.name
        map["msg"] = msg
        if(data!=null){
            map["data"] = JSONObject(data as Map<String, Any>?)
        }
        return map.toJSONString()
    }

    @Deprecated("Use FileReadUtil.readJson()")
    fun jsonFromFile(file: File): JSONObject? {
        val s: String
        try {
            val fileReader = FileReader(file)
            val reader = InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)
            var ch: Int
            val sb = StringBuffer()
            do {
                ch = reader.read()
                if(ch == -1){
                    break
                }
                sb.append(ch.toChar())
            } while (true)
            fileReader.close()
            reader.close()
            s = sb.toString()
            return JSONObject.parseObject(s)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun htmlTemplate(): String {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>####title-author####</title>\n" +
                "</head>\n" +
                "<style type=\"text/css\">\n" +
                "    ####style####\n" +
                "</style>\n" +
                "<body style=\"width: 85%; text-align: left\">\n" +
                "\n" +
                "<h1>####title####</h1>\n" +
                "<p>Last Edit: ####nickname#### @####last_edit_time####</p>\n" +
                "<blockquote id = \"introduction\">introduction:<br>####introduction####</blockquote>\n" +
                "<div id = \"content\">\n" +
                "####content####\n" +
                "</div>\n" +
                "\n" +
                "</body>\n" +
                "</html>"
    }

    fun markdownAirCss(): String {
        return "@media print {\n" +
                "        *,\n" +
                "        *:before,\n" +
                "        *:after {\n" +
                "            background: transparent !important;\n" +
                "            color: #000 !important;\n" +
                "            box-shadow: none !important;\n" +
                "            text-shadow: none !important;\n" +
                "        }\n" +
                "\n" +
                "        a,\n" +
                "        a:visited {\n" +
                "            text-decoration: underline;\n" +
                "        }\n" +
                "\n" +
                "        a[href]:after {\n" +
                "            content: \" (\" attr(href) \")\";\n" +
                "        }\n" +
                "\n" +
                "        abbr[title]:after {\n" +
                "            content: \" (\" attr(title) \")\";\n" +
                "        }\n" +
                "\n" +
                "        a[href^=\"#\"]:after,\n" +
                "        a[href^=\"javascript:\"]:after {\n" +
                "            content: \"\";\n" +
                "        }\n" +
                "\n" +
                "        pre,\n" +
                "        blockquote {\n" +
                "            border: 1px solid #999;\n" +
                "            page-break-inside: avoid;\n" +
                "        }\n" +
                "\n" +
                "        thead {\n" +
                "            display: table-header-group;\n" +
                "        }\n" +
                "\n" +
                "        tr,\n" +
                "        img {\n" +
                "            page-break-inside: avoid;\n" +
                "        }\n" +
                "\n" +
                "        img {\n" +
                "            max-width: 100% !important;\n" +
                "        }\n" +
                "\n" +
                "        p,\n" +
                "        h2,\n" +
                "        h3 {\n" +
                "            orphans: 3;\n" +
                "            widows: 3;\n" +
                "        }\n" +
                "\n" +
                "        h2,\n" +
                "        h3 {\n" +
                "            page-break-after: avoid;\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    html {\n" +
                "        font-size: 12px;\n" +
                "    }\n" +
                "\n" +
                "    @media screen and (min-width: 32rem) and (max-width: 48rem) {\n" +
                "        html {\n" +
                "            font-size: 15px;\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    @media screen and (min-width: 48rem) {\n" +
                "        html {\n" +
                "            font-size: 16px;\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    body {\n" +
                "        line-height: 1.85;\n" +
                "    }\n" +
                "\n" +
                "    p,\n" +
                "    .air-p {\n" +
                "        font-size: 1rem;\n" +
                "        margin-bottom: 1.3rem;\n" +
                "    }\n" +
                "\n" +
                "    h1,\n" +
                "    .air-h1,\n" +
                "    h2,\n" +
                "    .air-h2,\n" +
                "    h3,\n" +
                "    .air-h3,\n" +
                "    h4,\n" +
                "    .air-h4 {\n" +
                "        margin: 1.414rem 0 .5rem;\n" +
                "        font-weight: inherit;\n" +
                "        line-height: 1.42;\n" +
                "    }\n" +
                "\n" +
                "    h1,\n" +
                "    .air-h1 {\n" +
                "        margin-top: 0;\n" +
                "        font-size: 3.998rem;\n" +
                "    }\n" +
                "\n" +
                "    h2,\n" +
                "    .air-h2 {\n" +
                "        font-size: 2.827rem;\n" +
                "    }\n" +
                "\n" +
                "    h3,\n" +
                "    .air-h3 {\n" +
                "        font-size: 1.999rem;\n" +
                "    }\n" +
                "\n" +
                "    h4,\n" +
                "    .air-h4 {\n" +
                "        font-size: 1.414rem;\n" +
                "    }\n" +
                "\n" +
                "    h5,\n" +
                "    .air-h5 {\n" +
                "        font-size: 1.121rem;\n" +
                "    }\n" +
                "\n" +
                "    h6,\n" +
                "    .air-h6 {\n" +
                "        font-size: .88rem;\n" +
                "    }\n" +
                "\n" +
                "    small,\n" +
                "    .air-small {\n" +
                "        font-size: .707em;\n" +
                "    }\n" +
                "\n" +
                "    img,\n" +
                "    canvas,\n" +
                "    iframe,\n" +
                "    video,\n" +
                "    svg,\n" +
                "    select,\n" +
                "    textarea {\n" +
                "        max-width: 100%;\n" +
                "    }\n" +
                "\n" +
                "    body {\n" +
                "        color: #444;\n" +
                "        font-family: 'Open Sans', Helvetica, sans-serif;\n" +
                "        font-weight: 300;\n" +
                "        margin: 6rem auto 1rem;\n" +
                "        max-width: 48rem;\n" +
                "        text-align: center;\n" +
                "    }\n" +
                "\n" +
                "    img {\n" +
                "        border-radius: 50%;\n" +
                "        height: 200px;\n" +
                "        margin: 0 auto;\n" +
                "        width: 200px;\n" +
                "    }\n" +
                "\n" +
                "    a,\n" +
                "    a:visited {\n" +
                "        color: #3498db;\n" +
                "    }\n" +
                "\n" +
                "    a:hover,\n" +
                "    a:focus,\n" +
                "    a:active {\n" +
                "        color: #2980b9;\n" +
                "    }\n" +
                "\n" +
                "    pre {\n" +
                "        background-color: #fafafa;\n" +
                "        padding: 1rem;\n" +
                "        text-align: left;\n" +
                "    }\n" +
                "\n" +
                "    blockquote {\n" +
                "        margin: 0;\n" +
                "        border-left: 5px solid #7a7a7a;\n" +
                "        font-style: italic;\n" +
                "        padding: 0 0 0 1.33em;\n" +
                "        text-align: left;\n" +
                "    }\n" +
                "\n" +
                "    ul,\n" +
                "    ol,\n" +
                "    li {\n" +
                "        text-align: left;\n" +
                "    }\n" +
                "\n" +
                "    p {\n" +
                "        color: #777;\n" +
                "    }"
    }

}
