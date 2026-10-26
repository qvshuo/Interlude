package art.anjing.interlude.data

import java.text.Collator
import java.util.Locale

internal object AppOrder {
    private val priorityPackages = listOf("com.gitvdemo.video", "com.cibn.tv", "com.ktcp.video", "com.hunantv.market")

    fun <T> sort(items: List<T>, id: (T) -> String, label: (T) -> String, used: Map<String, Long>): List<T> {
        val collator = Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
        return items.sortedWith { a, b ->
            val recent = (used[id(b)] ?: 0L).compareTo(used[id(a)] ?: 0L)
            if (recent != 0) return@sortedWith recent
            val priority = priority(id(a)).compareTo(priority(id(b)))
            if (priority != 0) return@sortedWith priority
            val left = label(a).trim()
            val right = label(b).trim()
            val language = languageGroup(left).compareTo(languageGroup(right))
            if (language != 0) return@sortedWith language
            val name = if (languageGroup(left) == 0) collator.compare(left, right)
                else left.compareTo(right, ignoreCase = true)
            if (name != 0) name else id(a).compareTo(id(b))
        }
    }

    private fun priority(id: String): Int = priorityPackages.indexOf(id).takeIf { it >= 0 } ?: priorityPackages.size

    private fun languageGroup(name: String): Int =
        if (name.isNotEmpty() && Character.UnicodeScript.of(name.codePointAt(0)) == Character.UnicodeScript.HAN) 0 else 1

    fun nextTime(now: Long, previous: Collection<Long>): Long = maxOf(now, (previous.maxOrNull() ?: 0) + 1)
}
