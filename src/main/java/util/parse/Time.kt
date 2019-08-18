package util.parse

import util.CONF
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object Time {

    private fun getTime(s: String?): Date? {
        if (s == null) {
            return null
        }

        return try {
            val sdf = SimpleDateFormat(CONF.dateFormat, Locale.CHINA)
            sdf.parse(s)
        } catch (e: ParseException) {
            null
        }

    }

    fun getTime(date: Date): String {
        val time: String
        val sdf = SimpleDateFormat(CONF.dateFormat, Locale.CHINA)
        time = sdf.format(date)
        return time
    }

    fun String?.getFormattedTime(): String {
        val date = getTime(this)
        date?.let {
            return getFormattedTime(date.time)
        }
        return "unknown time"
    }

    fun Date.getFormattedTime(): String {
        return getFormattedTime(this.time)
    }

    private fun getFormattedTime(timestamp: Long): String {
        val result: String
        val todayCalendar = Calendar.getInstance()
        val otherCalendar = Calendar.getInstance()
        otherCalendar.timeInMillis = timestamp

        val timeFormat: String
        val yearTimeFormat: String
        var amPm = ""
        val hour = otherCalendar.get(Calendar.HOUR_OF_DAY)//判断当前是不是星期日     如想显示为：周日 12:09 可去掉此判断

        when {
            hour in 0..5 -> amPm = "凌晨"
            hour in 6..11 -> amPm = "早上"
            hour == 12 -> amPm = "中午"
            hour in 13..17 -> amPm = "下午"
            hour >= 18 -> amPm = "晚上"
        }
        timeFormat = "M月d日 " + amPm + "HH:mm"
        yearTimeFormat = "yyyy年M月d日 " + amPm + "HH:mm"

        val yearTemp = todayCalendar.get(Calendar.YEAR) == otherCalendar.get(Calendar.YEAR)
        if (yearTemp) {
            val todayMonth = todayCalendar.get(Calendar.MONTH)
            val otherMonth = otherCalendar.get(Calendar.MONTH)
            if (todayMonth == otherMonth) {//表示是同一个月
                val temp = todayCalendar.get(Calendar.DATE) - otherCalendar.get(Calendar.DATE)
                when (temp) {
                    0 -> result = getHourAndMin(timestamp)
                    1 -> result = "昨天 " + getHourAndMin(timestamp)
                    2, 3, 4, 5, 6 -> {
                        val dayOfMonth = otherCalendar.get(Calendar.WEEK_OF_MONTH)
                        val todayOfMonth = todayCalendar.get(Calendar.WEEK_OF_MONTH)
                        result = if (dayOfMonth == todayOfMonth) {//表示是同一周
                            val dayOfWeek = otherCalendar.get(Calendar.DAY_OF_WEEK)
                            if (dayOfWeek != 1) {//判断当前是不是星期日     如想显示为：周日 12:09 可去掉此判断
                                dayNames[otherCalendar.get(Calendar.DAY_OF_WEEK) - 1] + getHourAndMin(
                                    timestamp
                                )
                            } else {
                                getTime(timestamp, timeFormat)
                            }
                        } else {
                            getTime(timestamp, timeFormat)
                        }
                    }
                    else -> result = getTime(timestamp, timeFormat)
                }
            } else {
                result = getTime(timestamp, timeFormat)
            }
        } else {
            result = getYearTime(timestamp, yearTimeFormat)
        }
        return result
    }


    /**
     * 时间戳格式转换
     */
    private var dayNames = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

    /**
     * 当天的显示时间格式
     * @param time
     * @return
     */
    private fun getHourAndMin(time: Long): String {
        val format = SimpleDateFormat("HH:mm", Locale.CHINA)
        return format.format(Date(time))
    }

    /**
     * 不同一周的显示时间格式
     * @param time
     * @param timeFormat
     * @return
     */
    private fun getTime(time: Long, timeFormat: String): String {
        val format = SimpleDateFormat(timeFormat, Locale.CHINA)
        return format.format(Date(time))
    }

    /**
     * 不同年的显示时间格式
     * @param time
     * @param yearTimeFormat
     * @return
     */
    private fun getYearTime(time: Long, yearTimeFormat: String): String {
        val format = SimpleDateFormat(yearTimeFormat, Locale.CHINA)
        return format.format(Date(time))
    }

}