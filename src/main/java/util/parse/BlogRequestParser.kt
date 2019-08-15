package util.parse

import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.FileUploadException
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.servlet.ServletFileUpload
import util.CONF.Companion.conf
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.servlet.http.HttpServletRequest

class BlogRequestParser(req: HttpServletRequest) {
    private val files = HashMap<String, File>()
    private val factory = DiskFileItemFactory()
    private val fileUpload = ServletFileUpload(factory)
    private var list: MutableList<FileItem>? = null
    private val fields: HashMap<String, String> = HashMap()

    init {
        try {
            list = fileUpload.parseRequest(req)
        } catch (e: FileUploadException) {
            e.printStackTrace()
        }
    }

    fun getField(): HashMap<String, String> {
        if (list.isNullOrEmpty()) {
            return HashMap()
        }
        try {
            for (item in list!!) {
                if (item.isFormField) {
                    fields[item.fieldName] = item.string
                }
            }
        } catch (e: FileUploadException) {
            e.printStackTrace()
        }

        return fields
    }

    fun getBlogFiles(blogId: String): HashMap<String, File> {
        if (list.isNullOrEmpty()) {
            return HashMap()
        }
        try {
            for (item in list!!) {
                if (!item.isFormField) {
                    val file = File(item.name)
                    val outFile = File(conf.blog + "/" + blogId, file.name)
                    outFile.parentFile.mkdirs()
                    outFile.createNewFile()
                    val ins = item.inputStream
                    val ous = FileOutputStream(outFile)

                    try {
                        val buffer = ByteArray(1024)
                        var len: Int
                        do {
                            len = ins.read(buffer)
                            if (len == -1) {
                                break
                            }
                            ous.write(buffer, 0, len)
                        } while (true)
                        files[item.name] = outFile
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        ins.close()
                        ous.close()
                    }
                }
            }
        } catch (e: FileUploadException) {
            e.printStackTrace()
        }

        return files
    }


}