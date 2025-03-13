package com.advanced.bookmarks.advancedbookmarks.actions
import com.advanced.bookmarks.advancedbookmarks.StringBookmarkManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages

class AddStringBookmarkAction : AnAction("Add String Bookmark") {

    override fun actionPerformed(event: AnActionEvent) {
        // Get the project from the event
        val project = event.project ?: return

        // Get the bookmark manager service
        val manager = project.getService(StringBookmarkManager::class.java)

        // Get the editor from the event (this is the correct way)
        val editor = event.getData(CommonDataKeys.EDITOR)
        if (editor == null) {
            Messages.showErrorDialog(project, "No editor found", "Error")
            return
        }

        // Get the current document and caret position
        val document = editor.document
        val caret = editor.caretModel.primaryCaret

        // Get the virtual file associated with the document
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val filePath = file.path

        // Get the current line number where the caret is located
        val lineNumber = caret.logicalPosition.line

        // Show an input dialog for the user to enter their desired bookmark identifier
        val identifier = Messages.showInputDialog(
            project,
            "Enter Bookmark Identifier:",
            "Add Bookmark",
            Messages.getQuestionIcon()
        ) ?: return

        // Add the identifier and bookmark details to the bookmark manager
        manager.addBookmark(filePath, lineNumber, identifier)

        // Optionally, show a feedback notification
        Messages.showInfoMessage(
            project,
            "Bookmark added successfully at line ${lineNumber + 1} in $filePath",
            "Bookmark Added"
        )
    }
}