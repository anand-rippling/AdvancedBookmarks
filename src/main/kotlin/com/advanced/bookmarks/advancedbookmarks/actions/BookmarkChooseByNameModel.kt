package com.advanced.bookmarks.advancedbookmarks.actions

import com.advanced.bookmarks.advancedbookmarks.StringBookmarkManager
import com.intellij.ide.util.gotoByName.ChooseByNameModel
import com.intellij.openapi.project.Project
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.ListCellRenderer

class BookmarkChooseByName(
    private val project: Project,
    private val bookmarks: List<StringBookmarkManager.Bookmark>
) : ChooseByNameModel {

    override fun getPromptText(): String = "Search Bookmarks" // Text to display in the search popup.

    override fun getNotInMessage(): String = "No matching bookmarks found in the current scope"

    override fun getNotFoundMessage(): String = "No bookmarks match your query"

    override fun getCheckBoxName(): String? = null // No checkbox required; return null.

    override fun getFullName(element: Any): String {
        return when (element) {
            is StringBookmarkManager.Bookmark -> "Bookmark: ${element.name} at Line ${element.line} [${element.filePath}]"
            else -> ""
        }
    }

    override fun getElementName(element: Any): String? {
        if (element is StringBookmarkManager.Bookmark) {
            // Display the bookmark as "BookmarkName (filePath:line)"
            return "${element.name} (${element.filePath}:${element.line + 1})"
        }
        return null
    }

    override fun loadInitialCheckBoxState(): Boolean = false

    override fun getNames(p0: Boolean): Array<String> {
        // Return the names of items that can be searched, e.g., file paths or bookmark identifiers.
        return bookmarks.map { it.filePath }.toTypedArray()
    }


    override fun getListCellRenderer(): ListCellRenderer<Any> {
        return object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<out Any>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

                if (value is StringBookmarkManager.Bookmark) {
                    // Example Custom Display — File Name & Position
                    text = " ${value.name}: Line ${value.line} [${value.filePath}]"
                }
                return component
            }
        }
    }

    override fun getSeparators(): Array<String> = arrayOf() // Return no separators for this popup.
    override fun saveInitialCheckBoxState(state: Boolean) {
        // Save the checkbox state (if the checkbox is used). No-op for now.
    }
    override fun getElementsByName(name: String, includeNonProjectItems: Boolean, additionalSearchParam: String): Array<Any> {
        // Filter bookmarks by name (case-insensitive search).
        return bookmarks.filter { bookmark ->
            // Perform filtering:
            // - Match the file path with the search query (name).
            // - Additional filtering logic can be implemented using the "additionalSearchParam".
            bookmark.filePath.contains(name, ignoreCase = true)
        }.toTypedArray()
    }
    override fun getHelpId(): String? = null // No help context
    override fun willOpenEditor(): Boolean = true //
    override fun useMiddleMatching(): Boolean = true


}