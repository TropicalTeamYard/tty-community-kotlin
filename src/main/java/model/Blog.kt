package model

import java.util.*

interface Blog {
    val blogId: String
    val author: String
    val title: String
    val introduction: String
    val tag: String
    val lastEditTime: Date
    class Outline(
        override val blogId: String,
        override val author: String,
        override val title: String,
        override val introduction: String,
        override val tag: String,
        override val lastEditTime: Date
    ) : Blog {
        var index = -1
    }

    class Detail(
        override val blogId: String,
        override val author: String,
        override val title: String,
        override val introduction: String,
        override val tag: String,
        override val lastEditTime: Date
    ) : Blog {
        var content: String = ""
        var comment: String = ""
        var likes = ""
        var status = "deleted::0"
        var data: String? = null

    }
}
