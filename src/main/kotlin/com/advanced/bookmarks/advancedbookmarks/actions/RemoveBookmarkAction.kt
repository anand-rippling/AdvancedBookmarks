package com.advanced.bookmarks.advancedbookmarks.actions
import com.advanced.bookmarks.advancedbookmarks.StringBookmarkManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import java.awt.Component
import javax.swing.JOptionPane

class RemoveBookmarkAction : AnAction("Remove String Bookmark", "Search and Remove a Bookmark Permanently", null) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val bookmarkManager = project.getService(StringBookmarkManager::class.java)

        // Fetch all existing bookmarks
        val existingBookmarks = bookmarkManager.listBookmarks()

        if (existingBookmarks.isEmpty()) {
            // No bookmarks to display
            JOptionPane.showMessageDialog(null, "No bookmarks found to remove.", "Remove Bookmark", JOptionPane.INFORMATION_MESSAGE)
            return
        }

        // Create a popup to display and search bookmarks
        val popup = JBPopupFactory.getInstance()
            .createPopupChooserBuilder(existingBookmarks.values.toList())
            .setTitle("Remove Bookmark")
            .setRenderer(object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<out Any>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    if (value is StringBookmarkManager.Bookmark) {
                        // Display the bookmark details in a user-friendly format
                        text = "📍 ${value.name} - Line ${value.line} in ${value.filePath}"
                    }
                    return component
                }
            })
            // Handle the callback when a user selects a bookmark
            .setItemChosenCallback { chosenBookmark ->
                if (chosenBookmark is StringBookmarkManager.Bookmark) {
                    // Show a confirmation dialog before removing the bookmark
                    val confirmation = JOptionPane.showConfirmDialog(
                        null,
                        "Are you sure you want to remove the bookmark \"${chosenBookmark.name}\"?",
                        "Confirm Removal",
                        JOptionPane.YES_NO_OPTION
                    )
                    if (confirmation == JOptionPane.YES_OPTION) {
                        // Remove the selected bookmark
                        bookmarkManager.removeBookmark(chosenBookmark.name)
                        JOptionPane.showMessageDialog(
                            null,
                            "Bookmark \"${chosenBookmark.name}\" has been removed.",
                            "Bookmark Removed",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    }
                }
            }
            .createPopup()

        // Show the popup
        popup.showInFocusCenter()
    }
}