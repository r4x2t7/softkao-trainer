package com.softkao.trainer.feature.knowledge

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.softkao.trainer.core.data.VisualRepository
import com.softkao.trainer.core.model.VisualLesson
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
 * 把第四批抽象图解逐帧画成 PNG，用眼睛检查排版（节点重叠 / 文字出界）。
 *
 * 真机和模拟器都被占用，不能碰 adb —— 沿用 WrongScreenShotTest 的
 * Robolectric NATIVE 渲染方案，decorView 直接画到 Bitmap 上。
 * autoPlay 关掉，避免计时器把帧翻走导致截到哪帧全凭运气。
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = com.softkao.trainer.TestApp::class,
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConceptAnimationShotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val newLessonIds = listOf(
        "si-decision-tree",
        "si-sensitivity",
        "si-cost-type",
        "md-cdn",
        "md-drm",
        "md-mmedia-flow",
    )

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
        println("SHOT: ${out.absolutePath} ${bitmap.width}x${bitmap.height}")
    }

    private fun render(first: VisualLesson, current: androidx.compose.runtime.MutableState<VisualLesson>) {
        composeRule.setContent {
            SoftKaoTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp),
                ) {
                    // lesson.id 变化时 ConceptAnimation 内部 remember 会把帧位归零
                    ConceptAnimation(lesson = current.value, autoPlay = false)
                }
            }
        }
        current.value = first
        composeRule.waitForIdle()
    }

    private fun lesson(id: String): VisualLesson =
        VisualRepository.all().first { it.id == id }

    @Test
    fun `第四批每个图解逐帧渲染出图`() {
        val all = VisualRepository.all()
        assertTrue("第四批图解没有合并进 all()", all.any { it.id == "si-decision-tree" })

        val current = androidx.compose.runtime.mutableStateOf(all.first { it.id == newLessonIds.first() })
        render(current.value, current)

        newLessonIds.forEach { id ->
            val lesson = all.first { it.id == id }
            current.value = lesson
            composeRule.waitForIdle()
            assertTrue(
                "要点文本没有渲染出来（$id）",
                composeRule.onAllNodesWithText("要点：${lesson.takeaway}", substring = true)
                    .fetchSemanticsNodes().isNotEmpty(),
            )
            capture("anim-$id-f0")
            lesson.frames.indices.drop(1).forEach { frame ->
                composeRule.onNodeWithText("›").performClick()
                composeRule.waitForIdle()
                capture("anim-$id-f$frame")
            }
        }
    }
}
