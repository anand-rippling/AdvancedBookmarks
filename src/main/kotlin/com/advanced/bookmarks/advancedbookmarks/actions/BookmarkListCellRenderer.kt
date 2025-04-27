package com.advanced.bookmarks.advancedbookmarks.actions

import com.advanced.bookmarks.advancedbookmarks.StringBookmarkManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class BookmarkListCellRenderer : ListCellRenderer<StringBookmarkManager.Bookmark> {
    override fun getListCellRendererComponent(
        list: JList<out StringBookmarkManager.Bookmark>,
        value: StringBookmarkManager.Bookmark?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val panel = object : JPanel() {
            override fun getPreferredSize(): Dimension {
                // Always use the list's width for the renderer panel
                return Dimension(list.width, super.getPreferredSize().height)
            }
            override fun paintComponent(g: Graphics) {
                if (isSelected) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    val highlightWidth = list.width - 8 // 8 for padding
                    // Draw shadow
                    g2.color = Color(0, 0, 0, 40)
                    g2.fillRoundRect(6, 6, highlightWidth - 4, height - 8, 18, 18)
                    // Draw main highlight
                    g2.color = UIManager.getColor("List.selectionBackground") ?: Color(0x2D, 0x3A, 0x4A)
                    g2.fillRoundRect(4, 2, highlightWidth, height - 4, 16, 16)
                }
                super.paintComponent(g)
            }
        }
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.isOpaque = false
        panel.border = EmptyBorder(6, 12, 6, 12)

        if (value == null) return panel

        // File type icon logic
        val vFile = LocalFileSystem.getInstance().findFileByPath(value.filePath)
        val fileType = vFile?.let { FileTypeManager.getInstance().getFileTypeByFileName(it.name) }
        val icon = fileType?.icon ?: FileTypes.UNKNOWN.icon

        val nameLabel = JLabel(value.name)
        val fileLabel = JLabel("${value.filePath}:${value.line + 1}", icon, JLabel.LEFT)
        nameLabel.font = Font("Dialog", Font.BOLD, 14)
        fileLabel.font = Font("Dialog", Font.PLAIN, 12)
        nameLabel.foreground = if (isSelected) UIManager.getColor("List.selectionForeground") else list.foreground
        fileLabel.foreground = if (isSelected) UIManager.getColor("List.selectionForeground") else list.foreground

        panel.add(nameLabel)
        panel.add(fileLabel)

        // Add separator (optional, but with more margin)
        if (index != list.model.size - 1) {
            val sep = JSeparator(JSeparator.HORIZONTAL)
            sep.foreground = Color(220, 220, 220, 80)
            sep.maximumSize = Dimension(Int.MAX_VALUE, 1)
            sep.border = EmptyBorder(4, 0, 4, 0)
            panel.add(sep)
        }

        return panel
    }
} 