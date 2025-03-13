package com.advanced.bookmarks.advancedbookmarks


import com.intellij.openapi.components.*
import com.intellij.openapi.components.RoamingType.*
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.ConcurrentHashMap

@State(
    name = "StringBookmarkManager", // The schema key for configuration
    storages = [Storage(value = "StringBookmarks.xml", roamingType = DISABLED)]
)
@Service(Service.Level.PROJECT)
class StringBookmarkManager(private val project: Project) : PersistentStateComponent<StringBookmarkManager.State> {
    private val bookmarks: MutableMap<String, Bookmark> = ConcurrentHashMap()

    data class Bookmark(val filePath: String, val line: Int, val name: String)
    data class State(var data: MutableMap<String, String> = mutableMapOf())

    override fun getState(): State {
        val serializedData = bookmarks.mapValues { (_, bookmark) ->
            "${bookmark.filePath},${bookmark.line},${bookmark.name}" // Serialize Bookmark to a string
        }
        return State(serializedData.toMutableMap())
    }

    override fun loadState(state: State) {
        bookmarks.clear()
        bookmarks.putAll(state.data.mapValues { (_, value) ->
            val parts = value.split(",")
            Bookmark(filePath = parts[0], line = parts[1].toInt(), name = parts[2])
        })
    }
    fun addBookmark(filePath: String, line: Int, name: String) {
        require(filePath.isNotEmpty()) { "File path cannot be empty" }
        require(line > 0) { "Line number must be greater than 0" }
        require(name.isNotEmpty()) { "Bookmark name cannot be empty" }
        val bookmark = Bookmark(filePath, line, name)
        bookmarks[filePath + line] = bookmark // Assuming a unique key is constructed from `filePath + line`
    }

    fun getBookmark(identifier: String): Bookmark? {
        return bookmarks[identifier]
    }

    fun listBookmarks(): Map<String, Bookmark> {
        return bookmarks.mapValues { entry ->
            Bookmark(
                filePath = entry.value.filePath,
                line = entry.value.line,
                name = entry.value.name
            )
        }
    }

    fun removeBookmark(identifier: String) {
        bookmarks.remove(identifier)
    }
}