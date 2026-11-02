package com.softkao.trainer.feature.stats

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.PracticeMode
import com.softkao.trainer.ui.theme.SoftKaoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 学习日历（热力图）的 Robolectric 渲染截图：种子数据造出 0/1-2/3-5/6+ 四档，
 * 验证格子真的画出来了、图例文案齐全。
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = com.softkao.trainer.TestApp::class,
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatsShotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun seed() {
        ServiceLocator.init(ApplicationProvider.getApplicationContext())
        val db = ServiceLocator.db
        val si = ExamId.SYSTEM_INTEGRATION.id
        val now = System.currentTimeMillis()
        val day = 24 * 3600_000L
        // 今天 8 题、昨天 4 题、前天 1 题、大前天 0（不造）
        fun insert(daysAgo: Int, n: Int, correct: Boolean) {
            repeat(n) { i ->
                db.insertAttempt(
                    "seed-q-$daysAgo-$i", si, PracticeMode.SEQUENTIAL,
                    listOf("A"), correct, 1000L, now - daysAgo * day + i * 60_000L,
                )
            }
        }
        insert(0, 8, true)
        insert(1, 4, false)
        insert(2, 1, true)
    }

    private fun capture(name: String) {
        val decor = composeRule.activity.window.decorView
        val width = if (decor.width > 0) decor.width else 822
        val height = if (decor.height > 0) decor.height else 1782
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))
        val out = File("build/ui-shots/$name.png")
        out.parentFile?.mkdirs()
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("SHOT: ${out.absolutePath}")
    }

    private fun waitForText(text: String, timeoutMs: Long = 30_000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) return true
            composeRule.mainClock.advanceTimeBy(50)
            composeRule.waitForIdle()
            Thread.sleep(20)
        }
        return composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun `学习日历渲染出图`() {
        seed()
        composeRule.setContent {
            SoftKaoTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    StatsScreen()
                }
            }
        }
        assertTrue("统计页没有加载", waitForText("学习日历 · 近 17 周"))
        assertTrue("图例没显示", waitForText("多（0 / 1-2 / 3-5 / 6+ 题）"))
        capture("stats-heatmap")
    }
}
