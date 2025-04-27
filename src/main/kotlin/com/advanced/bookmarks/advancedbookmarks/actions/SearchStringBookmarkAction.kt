package com.advanced.bookmarks.advancedbookmarks.actions

import com.advanced.bookmarks.advancedbookmarks.StringBookmarkManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.psi.PsiFileFactory
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import java.awt.KeyboardFocusManager
import java.awt.KeyEventDispatcher
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import javax.swing.ScrollPaneConstants

class SearchStringBookmarkAction : AnAction("Search String Bookmarks") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: run {
            Messages.showErrorDialog("No active project found.", "Error")
            return
        }

        val manager = project.getService(StringBookmarkManager::class.java)
        val bookmarks = manager.listBookmarks().values.toList()
        if (bookmarks.isEmpty()) {
            Messages.showInfoMessage("No bookmarks are available.", "Info")
            return
        }

        val listModel = DefaultListModel<StringBookmarkManager.Bookmark>()
        bookmarks.forEach { listModel.addElement(it) }
        val bookmarkList = JBList(listModel).apply {
            cellRenderer = BookmarkListCellRenderer()
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }

        // Right panel: code preview (initially empty)
        val previewPanel = JPanel(BorderLayout())
        previewPanel.preferredSize = Dimension(600, 400)

        // Split pane: left (list), right (preview)
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = JBScrollPane(bookmarkList).apply { preferredSize = Dimension(250, 400) }
        splitPane.rightComponent = previewPanel
        splitPane.dividerLocation = 250
        splitPane.resizeWeight = 0.0

        // Search field (define before listeners so it's accessible)
        val searchField = JBTextField().apply {
            putClientProperty("JTextField.Search.Gap", 0)
            putClientProperty("JTextField.Search.Icon", true)
            putClientProperty("JTextField.Search.CancelIcon", true)
            putClientProperty("JTextField.Search.FindPopup", true)
            emptyText.text = "Search bookmarks..."
        }

        // Main panel: search field on top, split pane below
        val mainPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8)
            add(searchField, BorderLayout.NORTH)
            add(splitPane, BorderLayout.CENTER)
        }

        // Declare popup variable before handlers
        var popup: com.intellij.openapi.ui.popup.JBPopup? = null

        // Helper to update code preview
        fun updatePreview(bookmark: StringBookmarkManager.Bookmark?) {
            previewPanel.removeAll()
            if (bookmark == null) {
                previewPanel.revalidate()
                previewPanel.repaint()
                return
            }
            // Calculate snippet start line
            val vFile = LocalFileSystem.getInstance().findFileByPath(bookmark.filePath)
            val document = vFile?.let { FileDocumentManager.getInstance().getDocument(it) }
            val totalLines = document?.lineCount ?: 0
            val startLine = maxOf(0, bookmark.line - 50)
            val highlightLine = bookmark.line - startLine

            val editor = createCodePreviewEditor(bookmark, project)
            if (editor != null) {
                previewPanel.add(editor.component, BorderLayout.CENTER)
                // Scroll to and highlight the bookmarked line
                SwingUtilities.invokeLater {
                    val caretModel = editor.caretModel
                    caretModel.moveToLogicalPosition(com.intellij.openapi.editor.LogicalPosition(highlightLine, 0))
                    editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
                    if (highlightLine in 0 until editor.document.lineCount) {
                        editor.selectionModel.setSelection(
                            editor.document.getLineStartOffset(highlightLine),
                            editor.document.getLineEndOffset(highlightLine)
                        )
                    }
                }
            }
            previewPanel.revalidate()
            previewPanel.repaint()
        }

        // Only one ListSelectionListener for preview updates
        bookmarkList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val bookmark = bookmarkList.selectedValue
                updatePreview(bookmark)
            }
        }

        // Enter key navigation
        bookmarkList.inputMap.put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "navigate")
        bookmarkList.actionMap.put("navigate", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                val selectedBookmark = bookmarkList.selectedValue
                if (selectedBookmark != null) {
                    navigateToBookmark(selectedBookmark, project)
                    popup?.closeOk(null)
                }
            }
        })

        // Double-click navigation
        bookmarkList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedBookmark = bookmarkList.selectedValue
                    if (selectedBookmark != null) {
                        navigateToBookmark(selectedBookmark, project)
                        popup?.closeOk(null)
                    }
                }
            }
        })

        // In the search field's key listener, after updating the model and for focus transfer:
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    val selectedBookmark = bookmarkList.selectedValue
                    if (selectedBookmark != null) {
                        navigateToBookmark(selectedBookmark, project)
                        popup?.closeOk(null)
                    }
                    e.consume()
                }
                if (e.keyCode == KeyEvent.VK_DOWN || e.keyCode == KeyEvent.VK_TAB) {
                    if (bookmarkList.model.size > 0) {
                        bookmarkList.selectedIndex = 0
                        bookmarkList.requestFocusInWindow()
                    }
                    e.consume()
                }
            }
            override fun keyReleased(e: KeyEvent) {
                val searchText = searchField.text.lowercase()
                val filteredModel = DefaultListModel<StringBookmarkManager.Bookmark>()
                for (i in 0 until listModel.size()) {
                    val bookmark = listModel.getElementAt(i)
                    if (bookmark.name.contains(searchText, ignoreCase = true) || bookmark.filePath.contains(searchText, ignoreCase = true)) {
                        filteredModel.addElement(bookmark)
                    }
                }
                bookmarkList.model = filteredModel
                if (filteredModel.size() > 0) {
                    bookmarkList.selectedIndex = 0
                    // updatePreview will be called by the selection listener
                } else {
                    updatePreview(null)
                }
            }
        })

        // Show popup
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mainPanel, searchField)
            .setTitle("Search Bookmarks")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()

        popup.showCenteredInCurrentWindow(project)

        // Type-to-search: forward character input to searchField
        val keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val dispatcher = KeyEventDispatcher { e ->
            if (e.id == KeyEvent.KEY_TYPED && !searchField.hasFocus() && e.keyChar.isLetterOrDigit()) {
                searchField.requestFocusInWindow()
                searchField.text += e.keyChar
                searchField.caretPosition = searchField.text.length
                true
            } else {
                false
            }
        }
        keyboardFocusManager.addKeyEventDispatcher(dispatcher)
        popup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                keyboardFocusManager.removeKeyEventDispatcher(dispatcher)
            }
        })
    }

    private fun createCodePreviewEditor(bookmark: StringBookmarkManager.Bookmark, project: Project): EditorEx? {
        val vFile = LocalFileSystem.getInstance().findFileByPath(bookmark.filePath) ?: return null
        val document = FileDocumentManager.getInstance().getDocument(vFile) ?: return null

        // Get a snippet (50 lines before and after the bookmark)
        val totalLines = document.lineCount
        val startLine = maxOf(0, bookmark.line - 50)
        val endLine = minOf(totalLines - 1, bookmark.line + 50)
        val snippet = (startLine..endLine).joinToString("\n") { i ->
            val start = document.getLineStartOffset(i)
            val end = document.getLineEndOffset(i)
            document.getText(com.intellij.openapi.util.TextRange(start, end))
        }

        // Create a document for the snippet
        val snippetDocument = EditorFactory.getInstance().createDocument(snippet)

        // Create the editor
        val editor = EditorFactory.getInstance().createViewer(snippetDocument, project) as EditorEx

        // Set syntax highlighting
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName(vFile.name)
        val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType)
        editor.highlighter = highlighter

        editor.isViewer = true
        editor.setCaretEnabled(false)
        editor.settings.isLineNumbersShown = true
        editor.settings.isFoldingOutlineShown = false
        editor.settings.isLineMarkerAreaShown = false
        editor.settings.isIndentGuidesShown = false
        editor.settings.isCaretRowShown = false
        editor.settings.additionalLinesCount = 0

        return editor
    }

    private fun navigateToBookmark(bookmark: StringBookmarkManager.Bookmark, project: Project) {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(bookmark.filePath)
        if (virtualFile == null) {
            Messages.showErrorDialog("The bookmarked file could not be found: ${bookmark.filePath}", "Error")
            return
        }
        val fileEditorManager = FileDocumentManager.getInstance()
        val document = fileEditorManager.getDocument(virtualFile)
        val editorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
        editorManager.openFile(virtualFile, true)
        val editor = editorManager.selectedTextEditor
        if (editor != null) {
            try {
                editor.caretModel.moveToLogicalPosition(com.intellij.openapi.editor.LogicalPosition(bookmark.line, 0))
                editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
            } catch (e: Exception) {
                Messages.showErrorDialog("Failed to navigate to the bookmarked line.", "Error")
            }
        }
    }
}