package com.softkao.trainer.feature.learn

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
 * 学习中心两页的 Robolectric 原生渲染截图（沿用既有方案，不碰 adb）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = com.softkao.trainer.TestApp::class,
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LearnShotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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
    fun `学习中心列表渲染出图`() {
        ServiceLocator.init(ApplicationProvider.getApplicationContext())
        composeRule.setContent {
            SoftKaoTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    LearningScreen(onBack = {}, onOpenChapter = { _, _ -> })
                }
            }
        }
        assertTrue("章节列表没有加载", waitForText("信息化发展"))
        assertTrue("大纲统计没显示", waitForText("掌握 "))
        capture("learn-list")
    }

    @Test
    fun `章节学习页渲染出图`() {
        ServiceLocator.init(ApplicationProvider.getApplicationContext())
        composeRule.setContent {
            SoftKaoTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    ChapterLearnScreen(
                        examId = ExamId.SYSTEM_INTEGRATION.id,
                        chapterId = "si-l13",
                        onBack = {},
                        onPractice = { _, _ -> },
                        onOpenChapterDetail = { _, _ -> },
                    )
                }
            }
        }
        assertTrue("本章讲解没显示", waitForText("本章在讲什么"))
        assertTrue("记忆钩子没显示", waitForText("直观记忆"))
        assertTrue("练习入口没显示", waitForText("学完就练"))
        capture("learn-chapter")
    }
}
