package util.phrase

import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.FileUploadException
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.servlet.ServletFileUpload
import servlet.APIUser
import util.conn.MySQLConn
import util.enums.Shortcut
import util.file.FileReadUtil
import util.log.Log
import util.phrase.Value.json
import util.phrase.Value.random
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.sql.SQLException
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.collections.HashMap

class PortraitUpdater(private val req: HttpServletRequest) {
    private val conf = FileReadUtil.readJson(File(req.servletContext.getRealPath("/conf/dir")))
    private val factory = DiskFileItemFactory()
    private val fileUpload = ServletFileUpload(factory)
    private var list: MutableList<FileItem>? = null
    private val conn = MySQLConn.connection

    init {
        try {
            list = fileUpload.parseRequest(req)
        } catch (e: FileUploadException) {
            e.printStackTrace()
        }
    }

    fun submit(): String {

        var id: String? = null
        var token: String? = null
        val ip = APIUser.getIPAddr(req) ?: "0.0.0.0"
        val date = Date()

        if (list.isNullOrEmpty()) {
            return json(Shortcut.AE, "arguments mismatch")
        } else {
            try {
                for (item in list!!) {
                    if (item.isFormField) {
                        when (item.fieldName) {
                            "id" -> id = item.string
                            "token" -> token = item.string
                        }
                    }
                }

                if (id.isNullOrEmpty() || token.isNullOrEmpty() || ip.isEmpty() || ip == "0.0.0.0") {
                    return json(Shortcut.AE, "arguments mismatch")
                }

                val ps = conn.prepareStatement("select * from user where id = ? limit 1")
                ps.setString(1, id)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    if (token == Value.getMD5(rs.getString("token"))) {
                        val name = "${id}_${random()}"
                        var hasPortrait = false
                        for (item in list!!) {
                            if (!item.isFormField) {
                                val field = item.fieldName
                                if (field == "portrait") {
                                    try {
                                        val file = File(name)
                                        val outFile = File(conf.getString("root") + "/${conf.getString("portrait")}", file.name)
                                        outFile.parentFile.mkdirs()
                                        outFile.createNewFile()

                                        val ins = item.inputStream
                                        val ous = FileOutputStream(outFile)
                                        val buffer = ByteArray(1024) //缓冲字节
                                        var len: Int
                                        do {
                                            len = ins.read(buffer)
                                            if (len == -1) {
                                                break
                                            }
                                            ous.write(buffer, 0, len)
                                        } while (true)
                                        ins.close()
                                        ous.close()
                                        hasPortrait = true
                                        break
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }

                                }
                            }
                        }

                        rs.close()
                        ps.close()
                        if (hasPortrait) {
                            val ps1 = conn.prepareStatement("update user_detail set portrait = ? where id = ?")
                            ps1.setString(1, name)
                            ps1.setString(2, id)
                            ps1.executeUpdate()
                            Log.changePortrait(id, date, ip, true, name)
                            ps1.close()
                            return json(Shortcut.OK, "change portrait successfully")
                        } else {
                            return json(Shortcut.AE, "arguments mismatch")
                        }
                    } else {
                        rs.close()
                        ps.close()
                        Log.changePortrait(id, date, ip, false)
                        return json(Shortcut.TE, "invalid token")
                    }

                } else {
                    rs.close()
                    ps.close()
                    Log.changePortrait(id, date, ip, false)
                    return json(Shortcut.UNE, "user $id has not been registered.")
                }

            } catch (e: SQLException) {
                e.printStackTrace()
                return json(Shortcut.OTHER, "SQL ERROR")
            } catch (e: Exception) {
                e.printStackTrace()
                return json(Shortcut.OTHER, "unknown error")
            }

        }
    }


}