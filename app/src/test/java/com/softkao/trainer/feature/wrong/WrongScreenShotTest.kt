package com.softkao.trainer.feature.wrong

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
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.Mastery
import com.softkao.trainer.core.model.Option
import com.softkao.trainer.core.model.Question
import com.softkao.trainer.core.model.QuestionType
import com.softkao.trainer.core.model.WrongEntry
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
 * 用 Robolectric 的原生渲染把错题本画成 PNG。
 *
 * **为什么需要这个**：真机/模拟器正在被占用，不能用 adb；
 * 而这个页面全是 chip + 卡片布局，光看代码看不出排版对不对。
 * Robolectric NATIVE 图形模式能在 JVM 上真渲染一次并截出图。
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = com.softkao.trainer.TestApp::class,
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WrongScreenShotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun seed() {
        ServiceLocator.init(ApplicationProvider.getApplicationContext())
        val db = ServiceLocator.db
        val si = ExamId.SYSTEM_INTEGRATION
        val now = System.currentTimeMillis()

        val stems = listOf(
            "某项目计划价值 PV=1000 万元，挣值 EV=800 万元，实际成本 AC=900 万元，该项目的成本偏差 CV 是多少",
            "关于项目组合与项目集，下列说法正确的是哪一个",
            "在风险登记册中记录已识别风险，其作用不包括以下哪一项",
            "进度管理中，关键路径上的活动总时差等于多少",
            "采购合同中，履约担保与质量保证金的区别体现在哪",
        )
        stems.forEachIndexed { i, stem ->
            db.upsertQuestion(
                Question(
                    id = "si-shot-${i + 1}",
                    examId = si.id,
                    subject = "信息系统项目管理师",
                    type = QuestionType.SINGLE,
                    difficulty = 3,
                    chapter = "第三章 项目整合管理",
                    knowledgePoint = if (i % 2 == 0) "挣值管理" else "风险管理",
                    stem = stem,
                    options = listOf(
                        Option("A", "−100 万元，成本超支"),
                        Option("B", "100 万元，成本节约"),
                        Option("C", "−200 万元，进度落后"),
                        Option("D", "200 万元，进度提前"),
                    ),
                    answer = listOf("A"),
                    explanation = "CV = EV − AC = 800 − 900 = −100 万元，说明成本超支。",
                    aiExplanation = null,
                    source = "改编自教材",
                    tags = emptyList(),
                    caseStudy = null,
                    caseId = null,
                    subQuestions = emptyList(),
                ),
                now,
            )
        }

        val masteries = listOf(
            Mastery.NOT_MASTERED,
            Mastery.NOT_MASTERED,
            Mastery.SHADY,
            Mastery.MASTERED,
            Mastery.NOT_MASTERED,
        )
        masteries.forEachIndexed { i, m ->
            db.upsertWrongEntry(
                WrongEntry(
                    questionId = "si-shot-${i + 1}",
                    examId = si.id,
                    wrongCount = i + 1,
                    streak = 0,
                    mastery = m,
                    lastWrongAt = now - 86_400_000L,
                    // 前三道已经到期，后两道还没到复习时间
                    nextReviewAt = if (i < 3) now - 3_600_000L else now + 3 * 86_400_000L,
                    note = null,
                )
            )
        }

        db.toggleBookmark("si-shot-1", si.id, now)
        db.toggleBookmark("si-shot-4", si.id, now)
    }

    /** 直接把 Activity 的 decorView 画到 Bitmap 上（Robolectric 没有真正的窗口，captureToImage 用不了） */
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

    /** 主动轮询等某个文本出现：StateFlow 发射 → 重组 不是同步的，waitForIdle 一次不够 */
    private fun waitForText(text: String, timeoutMs: Long = 30_000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return true
            composeRule.mainClock.advanceTimeBy(50)
            composeRule.waitForIdle()
            Thread.sleep(20)
        }
        return composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun renderScreen() {
        composeRule.setContent {
            SoftKaoTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    WrongScreen(onStartPractice = { _, _, _ -> })
                }
            }
        }
    }

    @Test
    fun `错题本页面渲染出来`() {
        seed()
        renderScreen()
        val loaded = waitForText("全部错题 5")
        if (!loaded) {
            println("TREE:\n" + composeRule.onRoot().printToString())
            println("DB wrong=" + ServiceLocator.practiceRepository.wrongEntries().size)
            println("DB questions=" + ServiceLocator.questionRepository.count())
        }
        assertTrue("错题本数据没有加载出来", loaded)
        capture("wrong-robolectric")
    }

    @Test
    fun `切到收藏视图后能看到当前视图和清除入口`() {
        seed()
        renderScreen()
        assertTrue("错题本数据没有加载出来", waitForText("全部错题 5"))

        // 直接驱动 ViewModel，避免依赖点击后的重绘时序
        // （点击回调本身由 WrongFilterBarUiTest 覆盖）
        val vm = ViewModelProvider(composeRule.activity)[WrongViewModel::class.java]
        vm.selectMode(WrongListMode.BOOKMARK)

        val switched = waitForText("当前：★ 收藏 · 系统集成 · 2 题")
        val hasClear = composeRule.onAllNodesWithText("清除筛选").fetchSemanticsNodes().isNotEmpty()
        println("switched=$switched hasClear=$hasClear")
        if (!switched) {
            println("TREE:\n" + composeRule.onRoot().printToString())
        }
        assertTrue("切到收藏视图后摘要没更新", switched)
        assertTrue("收藏视图没有清除筛选入口", hasClear)
        capture("wrong-bookmark-view")
    }
}
