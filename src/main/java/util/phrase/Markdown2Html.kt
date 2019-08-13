package util.phrase

import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.ParserEmulationProfile
import com.vladsch.flexmark.util.options.MutableDataSet

object Markdown2Html {
    fun parse(content: String): String {
        val options = MutableDataSet()
        options.setFrom(ParserEmulationProfile.MARKDOWN)
        options.set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()
        val document = parser.parse(content)
        return renderer.render(document)
    }

}