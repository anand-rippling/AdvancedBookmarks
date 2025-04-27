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
        return object : ListCellRenderer<Any> {
            override fun getListCellRendererComponent(
                list: JList<out Any>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                if (value is StringBookmarkManager.Bookmark) {
                    // Panel to render both bookmark details and code snippet
                    val panel = javax.swing.JPanel()
                    panel.layout = javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS)

                    // Fetch Editor Colors Scheme
                    val colorsScheme = com.intellij.openapi.editor.colors.EditorColorsManager.getInstance().globalScheme

                    // Extract editor-specific colors
                    val backgroundColor = if (isSelected) {
                        colorsScheme.getColor(com.intellij.openapi.editor.colors.EditorColors.SELECTION_BACKGROUND_COLOR)
                            ?: colorsScheme.defaultBackground // Fallback to default editor background
                    } else {
                        colorsScheme.defaultBackground
                    }

                    val foregroundColor = if (isSelected) {
                        colorsScheme.getColor(com.intellij.openapi.editor.colors.EditorColors.SELECTION_FOREGROUND_COLOR)
                            ?: colorsScheme.defaultForeground // Fallback to default editor foreground
                    } else {
                        colorsScheme.defaultForeground
                    }

                    // Fetch Editor Font Dynamically
                    val editorFontAttributes = colorsScheme.getFontPreferences()
                    val editorFontName = editorFontAttributes.fontFamily
                    val editorFontSize = editorFontAttributes.getSize(editorFontName)
                    val editorFont = java.awt.Font(editorFontName, java.awt.Font.PLAIN, editorFontSize)

                    // Bookmark Details (Name, File Path, Line No.)
                    val bookmarkLabel = javax.swing.JLabel("📍 ${value.name} (Line ${value.line}) - [${value.filePath}]")
                    bookmarkLabel.font = editorFont.deriveFont(java.awt.Font.BOLD)
                    bookmarkLabel.foreground = foregroundColor

                    // Code Snippet in Editor Colors
                    val codeSnippetArea = javax.swing.JTextPane()
                    codeSnippetArea.contentType = "text/plain" // Render as plain text
                    codeSnippetArea.text = loadCodeSnippet(value.filePath, value.line)
                    codeSnippetArea.font = editorFont // Use dynamically fetched editor font
                    codeSnippetArea.isEditable = false // Non-editable
                    codeSnippetArea.background = backgroundColor // Editor's background color
                    codeSnippetArea.foreground = foregroundColor // Editor's foreground color

                    // Add padding and spacing
                    bookmarkLabel.border = javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)
                    codeSnippetArea.border = javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)

                    // Add all components to the panel
                    panel.add(bookmarkLabel)
                    panel.add(javax.swing.JSeparator(javax.swing.SwingConstants.HORIZONTAL)) // Separator line
                    panel.add(codeSnippetArea)

                    // Add a border around the result for visual separation
                    val borderColor = colorsScheme.getColor(com.intellij.openapi.editor.colors.EditorColors.LINE_NUMBERS_COLOR)
                        ?: java.awt.Color.GRAY // Fallback to gray if not available
                    panel.border = javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor), // Bottom border
                        javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5) // Padding inside the panel
                    )

                    // Set panel background
                    panel.background = backgroundColor

                    return panel
                }

                // Default renderer for non-bookmark objects
                return DefaultListCellRenderer().getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            }
        }
    }

    /**
     * Helper function to load code snippet around the given line from the file.
     */
    private fun loadCodeSnippet(filePath: String, line: Int): String {
        val virtualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(filePath)
            ?: return "<Cannot read file content>"
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(virtualFile)
            ?: return "<Cannot read document>"

        val totalLines = document.lineCount
        val startLine = maxOf(line - 2, 0)
        val endLine = minOf(line + 2, totalLines - 1)

        return (startLine..endLine).joinToString("\n") { lineIndex ->
            val lineStartOffset = document.getLineStartOffset(lineIndex)
            val lineEndOffset = document.getLineEndOffset(lineIndex)
            document.getText(com.intellij.openapi.util.TextRange(lineStartOffset, lineEndOffset)).trim()
        }
    }
//    override fun getListCellRenderer(): ListCellRenderer<Any> {
//        return object : DefaultListCellRenderer() {
//            override fun getListCellRendererComponent(
//                list: JList<out Any>?,
//                value: Any?,
//                index: Int,
//                isSelected: Boolean,
//                cellHasFocus: Boolean
//            ): Component {
//                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
//
//                if (value is StringBookmarkManager.Bookmark) {
//                    // Example Custom Display — File Name & Position
//                    text = " ${value.name}: Line ${value.line} [${value.filePath}]"
//                }
//                return component
//            }
//        }
//    }

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