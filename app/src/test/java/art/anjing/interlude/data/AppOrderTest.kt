package art.anjing.interlude.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOrderTest {
    private data class App(val id: String, val name: String)
    private val apps = listOf(App("b", "视频"), App("c", "音乐"), App("a", "电视"))

    @Test fun recentAppsComeFirstAndUnknownAppsRemainStable() {
        val order = AppOrder.sort(apps, App::id, App::name, mapOf("c" to 20, "b" to 10, "removed" to 99))
        assertEquals(listOf("c", "b", "a"), order.map { it.id })
    }

    @Test fun duplicateLabelsUseStablePackageIdentity() {
        val items = listOf(App("b", "应用"), App("a", "应用"))
        assertEquals(listOf("a", "b"), AppOrder.sort(items, App::id, App::name, emptyMap()).map { it.id })
    }

    @Test fun defaultOrderUsesPackagePriorityThenChinesePinyinThenEnglishAlphabet() {
        val items = listOf(
            App("z", "zebra"), App("b", "帮助"), App("a", "apple"), App("y", "音乐"),
            App("com.hunantv.market", "芒果TV"), App("com.ktcp.video", "云视听极光"),
            App("com.cibn.tv", "CIBN酷喵"), App("com.gitvdemo.video", "银河奇异果"),
            App("d", "电视"), App("fake", "银河奇异果"),
        )
        assertEquals(listOf("com.gitvdemo.video", "com.cibn.tv", "com.ktcp.video", "com.hunantv.market",
            "b", "d", "y", "fake", "a", "z"),
            AppOrder.sort(items, App::id, App::name, emptyMap()).map { it.id })
    }

    @Test fun recentLaunchesOverridePackagePriorityAndLanguage() {
        val items = listOf(App("com.gitvdemo.video", "银河奇异果"), App("english", "Emby"),
            App("chinese", "帮助"), App("com.cibn.tv", "酷喵"))
        assertEquals(listOf("english", "chinese", "com.gitvdemo.video", "com.cibn.tv"),
            AppOrder.sort(items, App::id, App::name, mapOf("english" to 20, "chinese" to 10)).map { it.id })
    }

    @Test fun absentPriorityPackagesAndEnglishCaseDoNotAffectOrdering() {
        val items = listOf(App("z", "beta"), App("b", "Alpha"), App("a", "alpha"), App("c", "电视"))
        assertEquals(listOf("c", "a", "b", "z"),
            AppOrder.sort(items, App::id, App::name, emptyMap()).map { it.id })
    }

    @Test fun clockRollbackAndRapidLaunchesCannotReverseRecency() {
        assertEquals(101L, AppOrder.nextTime(10, listOf(100)))
        assertEquals(101L, AppOrder.nextTime(100, listOf(100)))
        assertEquals(200L, AppOrder.nextTime(200, emptyList()))
    }
}
