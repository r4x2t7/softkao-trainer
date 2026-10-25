package com.softkao.trainer.feature.wrong

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.Mastery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 错题本筛选栏的界面测试。
 *
 * 重点验证「点了有反馈」：
 * - 每个 chip 都带数量，点之前就知道会筛出什么
 * - 选中态说实话：选中的那个 chip 必须是 selected
 * - 有筛选时给出「清除筛选」入口
 * - 结果为空时说清楚为什么
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = com.softkao.trainer.TestApp::class)
class WrongFilterBarUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val counts = mapOf(
        Mastery.NOT_MASTERED to 5,
        Mastery.SHADY to 3,
        Mastery.MASTERED to 4,
    )

    private fun setBar(
        filter: WrongFilterState = WrongFilterState(),
        visibleCount: Int = 12,
        onSelectMode: (WrongListMode) -> Unit = {},
        onSelectMastery: (Mastery?) -> Unit = {},
        onClear: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                WrongFilterBar(
                    filter = filter,
                    examId = ExamId.SYSTEM_INTEGRATION,
                    masteryCounts = counts,
                    bookmarkCount = 2,
                    totalWrong = 12,
                    visibleCount = visibleCount,
                    onSelectMode = onSelectMode,
                    onSelectMastery = onSelectMastery,
                    onExamChange = {},
                    onClear = onClear,
                )
            }
        }
    }

    @Test
    fun `每个筛选chip都显示数量`() {
        setBar()
        composeRule.onNodeWithText("全部错题 12").assertIsDisplayed()
        composeRule.onNodeWithText("未掌握 5").assertIsDisplayed()
        composeRule.onNodeWithText("模糊 3").assertIsDisplayed()
        composeRule.onNodeWithText("已掌握 4").assertIsDisplayed()
        composeRule.onNodeWithText("★ 收藏 2").assertIsDisplayed()
    }

    @Test
    fun `点掌握度chip会回调`() {
        var picked: Mastery? = null
        setBar(onSelectMastery = { picked = it })
        composeRule.onNodeWithText("未掌握 5").performClick()
        composeRule.runOnIdle { assertEquals(Mastery.NOT_MASTERED, picked) }
    }

    @Test
    fun `点收藏chip会切到收藏视图`() {
        var mode: WrongListMode? = null
        setBar(onSelectMode = { mode = it })
        composeRule.onNodeWithText("★ 收藏 2").performClick()
        composeRule.runOnIdle { assertEquals(WrongListMode.BOOKMARK, mode) }
    }

    @Test
    fun `选中的chip显示为选中其它不选中`() {
        setBar(filter = WrongFilterState().withMastery(Mastery.SHADY))
        composeRule.onNodeWithText("模糊 3").assertIsSelected()
        composeRule.onNodeWithText("全部错题 12").assertIsNotSelected()
        composeRule.onNodeWithText("★ 收藏 2").assertIsNotSelected()
    }

    @Test
    fun `当前视图摘要写清楚筛出了多少题`() {
        setBar(filter = WrongFilterState().withMastery(Mastery.SHADY), visibleCount = 3)
        composeRule.onNodeWithText("当前：模糊 · 系统集成 · 3 题").assertIsDisplayed()
    }

    @Test
    fun `有筛选时出现清除筛选入口并能回调`() {
        var cleared = false
        setBar(filter = WrongFilterState().withMastery(Mastery.SHADY), onClear = { cleared = true })
        composeRule.onNodeWithText("清除筛选").assertIsDisplayed()
        composeRule.onNodeWithText("清除筛选").performClick()
        composeRule.runOnIdle { assertTrue("点了清除筛选没有回调", cleared) }
    }

    @Test
    fun `没有筛选时不出现清除筛选入口`() {
        setBar()
        composeRule.onNodeWithText("清除筛选").assertDoesNotExist()
        composeRule.onNodeWithText("当前：全部错题 · 系统集成 · 12 题").assertIsDisplayed()
    }

    @Test
    fun `科目切换也有反馈`() {
        var picked: ExamId? = null
        composeRule.setContent {
            MaterialTheme {
                WrongFilterBar(
                    filter = WrongFilterState(),
                    examId = ExamId.SYSTEM_INTEGRATION,
                    masteryCounts = counts,
                    bookmarkCount = 2,
                    totalWrong = 12,
                    visibleCount = 12,
                    onSelectMode = {},
                    onSelectMastery = {},
                    onExamChange = { picked = it },
                    onClear = {},
                )
            }
        }
        composeRule.onNodeWithText("多媒体").performClick()
        composeRule.runOnIdle { assertEquals(ExamId.MULTIMEDIA_DESIGNER, picked) }
    }

    // ---------------------------------------------------------------- 空状态

    @Test
    fun `筛选结果为空时说明原因并给清除入口`() {
        var cleared = false
        composeRule.setContent {
            MaterialTheme {
                WrongEmptyState(
                    filter = WrongFilterState().withMastery(Mastery.MASTERED),
                    totalWrong = 12,
                    examName = "系统集成",
                    onClear = { cleared = true },
                    onPractice = {},
                )
            }
        }
        composeRule.onNodeWithText("没有「已掌握」的错题").assertIsDisplayed()
        composeRule.onNodeWithText("看全部错题").assertIsDisplayed()
        composeRule.onNodeWithText("看全部错题").performClick()
        composeRule.runOnIdle { assertTrue("空状态里的清除入口没有回调", cleared) }
    }

    @Test
    fun `没有收藏时说明怎么用收藏`() {
        composeRule.setContent {
            MaterialTheme {
                WrongEmptyState(
                    filter = WrongFilterState().withMode(WrongListMode.BOOKMARK),
                    totalWrong = 12,
                    examName = "系统集成",
                    onClear = {},
                    onPractice = {},
                )
            }
        }
        composeRule.onNodeWithText("还没有收藏的题").assertIsDisplayed()
    }

    @Test
    fun `一个错题都没有时给出行动指引`() {
        composeRule.setContent {
            MaterialTheme {
                WrongEmptyState(
                    filter = WrongFilterState(),
                    totalWrong = 0,
                    examName = "系统集成",
                    onClear = {},
                    onPractice = {},
                )
            }
        }
        composeRule.onNodeWithText("还没有错题").assertIsDisplayed()
        composeRule.onNodeWithText("去练系统集成题").assertIsDisplayed()
    }
}
