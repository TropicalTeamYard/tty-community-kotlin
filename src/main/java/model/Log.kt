package model

import util.StringUtil

import java.util.Date

internal object Log {
    fun register(date: Date, ip: String, nickname: String): String {
        // user/register&nickname/feifei&time/2019-7-14 19:04:55&ip/192.168.123.186
        return "user/register&nickname/$nickname&time/$date&ip/$ip\n"
    }
}
