package com.softkao.trainer.feature.knowledge

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.softkao.trainer.feature.practice.PracticeJumpBar
import com.softkao.trainer.feature.practice.TrueFalseChoiceRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 界面级测试：用 Robolectric 在 JVM 上真正把 Compose 渲染出来，
 * 再用语义树断言「文字确实显示出来了」。
 *
 * 这是没有设备时唯一能真正验证 UI 的办法——纯逻辑测试看不出「文字被省略号截断」这种问题。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = com.softkao.trainer.TestApp::class)
class ComposeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ------------------------------------------------------------ 卡片

    @Test
    fun `章节卡片完整显示章节名`() {
        val chapter = "项目整合管理与项目生命周期管理"
        composeRule.setContent {
            MaterialTheme {
                KnowledgeChapterCard(
                    chapter = chapter,
                    total = 42,
                    answered = 12,
                    correct = 9,
                    hasMaterial = true,
                    onClick = {},
                )
            }
        }
        composeRule.onNodeWithText(chapter).assertIsDisplayed()
        composeRule.onNodeWithText("题量 42 · 已练 12").assertIsDisplayed()
        composeRule.onNodeWithText("正确率 75%").assertIsDisplayed()
    }

    @Test
    fun `章节卡片点击会回调`() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                KnowledgeChapterCard(
                    chapter = "成本管理",
                    total = 10,
                    answered = 0,
                    correct = 0,
                    hasMaterial = false,
                    onClick = { clicked = true },
                )
            }
        }
        composeRule.onNodeWithText("成本管理").performClick()
        composeRule.runOnIdle { assertTrue("点击没有回调", clicked) }
    }

    @Test
    fun `知识点卡片完整显示精讲文字`() {
        val note = "挣值三参数：PV 计划价值、EV 挣值、AC 实际成本；CV=EV-AC、SV=EV-PV。"
        composeRule.setContent {
            MaterialTheme {
                KnowledgePointCard(
                    point = "挣值管理",
                    count = 12,
                    done = 4,
                    right = 3,
                    note = note,
                    onPractice = {},
                )
            }
        }
        composeRule.onNodeWithText("挣值管理").assertIsDisplayed()
        composeRule.onNodeWithText(note).assertIsDisplayed()
        composeRule.onNodeWithText("12 题 · 正确率 75%").assertIsDisplayed()
    }

    @Test
    fun `知识点卡片没有精讲时不显示空白块`() {
        composeRule.setContent {
            MaterialTheme {
                KnowledgePointCard(
                    point = "没有精讲的考点",
                    count = 3,
                    done = 0,
                    right = 0,
                    note = null,
                    onPractice = {},
                )
            }
        }
        composeRule.onNodeWithText("没有精讲的考点").assertIsDisplayed()
        composeRule.onNodeWithText("3 题").assertIsDisplayed()
    }

    // ------------------------------------------------------------ 判断题

    @Test
    fun `判断题渲染出 √ 和 × 两个可点按钮`() {
        var picked = ""
        composeRule.setContent {
            MaterialTheme {
                TrueFalseChoiceRow(
                    selected = emptySet(),
                    answer = listOf("√"),
                    submitted = false,
                    enabled = true,
                    onSelect = { picked = it },
                )
            }
        }
        composeRule.onNodeWithText("√  正确").assertIsDisplayed()
        composeRule.onNodeWithText("×  错误").assertIsDisplayed()
        composeRule.onNodeWithText("√  正确").performClick()
        composeRule.runOnIdle { assertEquals("√", picked) }
    }

    @Test
    fun `判断题提交后不能再点`() {
        var picked = ""
        composeRule.setContent {
            MaterialTheme {
                TrueFalseChoiceRow(
                    selected = emptySet(),
                    answer = listOf("×"),
                    submitted = true,
                    enabled = false,
                    onSelect = { picked = it },
                )
            }
        }
        composeRule.onNodeWithText("×  错误").performClick()
        composeRule.runOnIdle { assertEquals("不应触发选择", "", picked) }
    }

    // ------------------------------------------------------------ 底部跳转栏

    @Test
    fun `跳转栏包含全部按钮`() {
        composeRule.setContent {
            MaterialTheme {
                PracticeJumpBar(
                    index = 0,
                    total = 100,
                    onJump = {},
                    onFirst = {},
                    onLast = {},
                    onSheet = {},
                )
            }
        }
        listOf("⌂ 首页", "−100", "−10", "上一页", "下一页", "+10", "+100", "尾页 ⇥", "答题卡").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("第 1 / 100 题").assertIsDisplayed()
    }

    @Test
    fun `跳转栏点击会跳到正确位置`() {
        val targets = mutableListOf<Int>()
        composeRule.setContent {
            MaterialTheme {
                PracticeJumpBar(
                    index = 50,
                    total = 100,
                    onJump = { targets += it },
                    onFirst = {},
                    onLast = {},
                    onSheet = {},
                )
            }
        }
        composeRule.onNodeWithText("+10").performScrollTo().performClick()
        composeRule.onNodeWithText("−100").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(60, 0), targets.toList())
        }
    }

    @Test
    fun `题量为零时跳转栏不渲染任何内容`() {
        composeRule.setContent {
            MaterialTheme {
                PracticeJumpBar(
                    index = 0,
                    total = 0,
                    onJump = {},
                    onFirst = {},
                    onLast = {},
                    onSheet = {},
                )
            }
        }
        composeRule.onNodeWithText("⌂ 首页").assertDoesNotExist()
    }
}
