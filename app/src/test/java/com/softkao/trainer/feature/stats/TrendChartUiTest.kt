package com.softkao.trainer.feature.stats

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 趋势图渲染测试：确认图例、坐标刻度都能真正画出来，且 0 值不会崩。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = com.softkao.trainer.TestApp::class)
class TrendChartUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `坐标轴刻度是整数所以不会画出小数标签`() {
        listOf(1, 3, 7, 8, 9, 13, 15, 33, 101, 250).forEach { v ->
            val n = niceMax(v)
            assertTrue("niceMax($v)=$n 必须不小于原值", n >= v)
            assertEquals("niceMax($v)=$n 必须是 4 的倍数，四等分才落在整数上", 0, n % 4)
        }
    }

    @Test
    fun `图例三个项目都显示出来`() {
        composeRule.setContent {
            MaterialTheme {
                TrendChart(
                    points = listOf(
                        TrendPoint("10-19", 0, 0),
                        TrendPoint("10-20", 3, 2),
                        TrendPoint("10-21", 9, 5),
                    ),
                    modifier = Modifier.width(320.dp),
                )
            }
        }
        composeRule.onNodeWithText("做题量").assertIsDisplayed()
        composeRule.onNodeWithText("答对数").assertIsDisplayed()
        composeRule.onNodeWithText("趋势").assertIsDisplayed()
    }

    @Test
    fun `没有任何数据时给出提示而不是空白`() {
        composeRule.setContent {
            MaterialTheme {
                TrendChart(points = emptyList(), modifier = Modifier.width(320.dp))
            }
        }
        composeRule.onNodeWithText("暂无做题记录").assertIsDisplayed()
    }

    @Test
    fun `全是 0 值时也能正常渲染`() {
        composeRule.setContent {
            MaterialTheme {
                TrendChart(
                    points = (1..7).map { TrendPoint("10-$it", 0, 0) },
                    modifier = Modifier.width(320.dp),
                )
            }
        }
        composeRule.onNodeWithText("趋势").assertIsDisplayed()
    }
}
