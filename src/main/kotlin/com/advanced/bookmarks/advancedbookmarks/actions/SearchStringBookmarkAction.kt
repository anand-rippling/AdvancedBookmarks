package com.advanced.bookmarks.advancedbookmarks.actions
import com.advanced.bookmarks.advancedbookmarks.StringBookmarkManager
import com.advanced.bookmarks.advancedbookmarks.actions.BookmarkChooseByName
import com.intellij.ide.util.gotoByName.ChooseByNamePopup
import com.intellij.ide.util.gotoByName.ChooseByNamePopupComponent
import com.intellij.ide.util.gotoByName.ChooseByNameItemProvider
import com.intellij.ide.util.gotoByName.ChooseByNameViewModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.Processor

class SearchStringBookmarkAction : AnAction("Search String Bookmark") {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: run {
            Messages.showErrorDialog("No active project found.", "Error")
            return
        }

        // Obtain the bookmark manager
        val manager = project.getService(StringBookmarkManager::class.java)
        val bookmarks = manager.listBookmarks().values.toList()

        if (bookmarks.isEmpty()) {
            Messages.showInfoMessage("No bookmarks are available.", "Info")
            return
        }

        // Create ChooseByNameModel
        val model = BookmarkChooseByName(project, bookmarks)

        // Create a custom ChooseByNameItemProvider
        val provider = object : ChooseByNameItemProvider {


            override fun filterNames(
                p0: ChooseByNameViewModel,
                p1: Array<out String>,
                p2: String
            ): MutableList<String> {
                val filteredNames = mutableListOf<String>()

                bookmarks.forEach { bookmark ->
                    val displayName = "${bookmark.name} (${bookmark.filePath}:${bookmark.line + 1})"

                    // Check if the bookmark name or its display name matches the search pattern (`p2`)
                    if (bookmark.name.contains(p2, ignoreCase = true) || displayName.contains(p2, ignoreCase = true)) {
                        filteredNames.add(displayName)
                    }
                }

                // Return the filtered list of names
                return filteredNames
            }

            override fun filterElements(
                model: ChooseByNameViewModel,
                pattern: String,
                everywhere: Boolean,
                indicator: ProgressIndicator,
                processor: Processor<Any>
            ): Boolean {
                bookmarks.forEach { bookmark ->
                    if (indicator.isCanceled) return false

                    val displayName = "${bookmark.name} (${bookmark.filePath}:${bookmark.line + 1})"
                    // Allow search by either the name or the combined display name
                    if (bookmark.name.contains(pattern, ignoreCase = true) || displayName.contains(pattern, ignoreCase = true)) {
                        if (!processor.process(bookmark)) {
                            return false
                        }
                    }
                }
                return true
            }


        }



        // Create the ChooseByNamePopup
        val popup = ChooseByNamePopup.createPopup(
            project,
            model,
            provider,
            "",          // Predefined text
        )
        popup.invoke(
            object : ChooseByNamePopupComponent.Callback() {
                override fun elementChosen(element: Any?) {
                    element?.let {
                        if (it is StringBookmarkManager.Bookmark) {
                            navigateToBookmark(it, project)
                        }
                    }
                }
            },
            ModalityState.defaultModalityState(),
            false // This boolean corresponds to allowMultipleSelection.
        )


    }

    private fun navigateToBookmark(bookmark: StringBookmarkManager.Bookmark, project: Project) {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(bookmark.filePath)
        if (virtualFile == null) {
            Messages.showErrorDialog("The bookmarked file could not be found: ${bookmark.filePath}", "Error")
            return
        }

        // Open the file in the editor
        val fileEditorManager = FileEditorManager.getInstance(project)
        fileEditorManager.openFile(virtualFile, true)

        val editor: Editor? = fileEditorManager.selectedTextEditor
        if (editor == null) {
            Messages.showErrorDialog("Failed to open the file in the editor.", "Error")
            return
        }

        // Move the caret to the bookmarked line
        try {
            editor.caretModel.moveToLogicalPosition(LogicalPosition(bookmark.line, 0))
            editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
        } catch (e: Exception) {
            Messages.showErrorDialog("Failed to navigate to the bookmarked line.", "Error")
        }
    }
}