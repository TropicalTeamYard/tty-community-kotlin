package util.log

import util.Value
import util.enums.LoginPlatform
import java.util.*

object Token {
    fun getToken(id: String, platform: LoginPlatform, secret: String, time: Date, status: Boolean): String {
        return "$id::${platform.name}::$secret::${Value.getTime(time)}::$status"
    }
}