package com.softkao.trainer.feature.casestudy

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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.Option
import com.softkao.trainer.core.model.Question
import com.softkao.trainer.core.model.QuestionType
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
 * 案例分析模块的 Robolectric 原生渲染截图（沿用 WrongScreenShotTest 的方案，
 * 真机/模拟器被占用不能碰 adb）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = com.softkao.trainer.TestApp::class,
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CaseStudyShotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun seed() {
        ServiceLocator.init(ApplicationProvider.getApplicationContext())
        val db = ServiceLocator.db
        val si = ExamId.SYSTEM_INTEGRATION
        val now = System.currentTimeMillis()

        fun caseQuestion(no: Int, stem: String) = Question(
            id = "si-casetest-$no",
            examId = si.id,
            subject = "案例分析",
            type = QuestionType.CASE,
            difficulty = 4,
            chapter = "项目质量管理",
            knowledgePoint = "质量整改案例分析",
            stem = "小问（$no）：$stem",
            options = listOf(
                Option("A", "小问（1）"),
                Option("B", "小问（2）"),
            ),
            answer = listOf("A"),
            explanation = "参考答案：先定位缺陷根因，再按质量保证流程整改并回归验证，最后更新质量保证计划。",
            aiExplanation = "AI 深度解析：本题考查质量管理过程的闭环。正确思路是先做根因分析（如因果图、5Why），再走纠正措施流程，修补后必须回归测试确认缺陷已消除，并把经验教训固化进质量保证计划防止复发。",
            source = "自编（测试用）",
            tags = listOf("案例", "质量管理"),
            caseStudy = "某系统集成公司承建的政务平台项目进入验收阶段，建设单位在初验中发现多个模块存在数据一致性缺陷，项目组直接安排开发人员逐个修补，未做根因分析，修补后也未回归测试。",
            caseId = "si-casetest-01",
            subQuestions = emptyList(),
        )

        db.upsertQuestion(caseQuestion(1, "指出项目组在质量整改过程中的三个主要问题。"), now)
        db.upsertQuestion(caseQuestion(2, "给出规范的质量整改流程。"), now)
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
        println("SHOT: ${out.absolutePath} ${bitmap.width}x${bitmap.height}")
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
    fun `案例列表渲染出图`() {
        seed()
        composeRule.setContent {
            SoftKaoTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    CaseListScreen(onBack = {}, onOpenCase = { _, _ -> })
                }
            }
        }
        assertTrue("案例列表没有加载出来", waitForText("质量整改案例分析"))
        assertTrue("汇总数字没显示", waitForText("1 组"))
        assertTrue("自评进度没显示", waitForText("0 问"))
        capture("case-list")
    }

    @Test
    fun `案例详情自评流渲染出图`() {
        seed()
        composeRule.setContent {
            SoftKaoTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    CaseDetailScreen(
                        examId = ExamId.SYSTEM_INTEGRATION.id,
                        caseId = "si-casetest-01",
                        onBack = {},
                    )
                }
            }
        }
        assertTrue("案例背景没显示", waitForText("案例背景"))
        assertTrue("小问没显示", waitForText("小问 1"))

        // 展开第一小问：参考答案 + AI 深度解析 + 自评按钮
        composeRule.onAllNodesWithText("自己作答后，点此对照参考答案")[0].performClick()
        assertTrue("参考答案没有展开", waitForText("参考答案"))
        assertTrue("AI 深度解析没有显示", waitForText("AI 深度解析"))
        capture("case-detail-revealed")

        // 自评「没答上」→ 应出现已记录提示
        composeRule.onNodeWithText("没答上").performClick()
        assertTrue("自评后没有出现记录提示", waitForText("已记录。"))
        capture("case-detail-graded")
    }
}
