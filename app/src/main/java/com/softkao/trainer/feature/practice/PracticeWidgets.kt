package com.softkao.trainer.feature.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.softkao.trainer.core.ui.PracticeLogic

/**
 * 刷题页里可以独立测试的部件。
 *
 * 判断题在题库里不带 options，界面上要渲染成 √/× 两个可点按钮；
 * 底部跳转栏则是用户点名要的「像页码一样跳题」。
 * 抽出来是为了能在 JVM 上直接渲染并断言。
 */

@Composable
fun TrueFalseChoiceRow(
    selected: Set<String>,
    answer: List<String>,
    submitted: Boolean,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PracticeLogic.TRUE_FALSE_CHOICES.forEach { (key, label) ->
            val isSelected = key in selected
            val isAnswer = key in answer
            val borderColor = when {
                submitted && isAnswer -> MaterialTheme.colorScheme.tertiary
                submitted && isSelected -> MaterialTheme.colorScheme.error
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable(enabled = enabled) { onSelect(key) }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$key  $label",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * 底部跳转栏：⌂首页 / 答题卡 / −100 / −10 / 上一页 / 第 x / y 题 / 下一页 / +10 / +100 / 尾页
 *
 * 分两行放，一屏全显示——之前挤在一行里要横向滚动才能看到「答题卡」和「尾页」，
 * 用户根本找不到这两个入口。
 */
@Composable
fun PracticeJumpBar(
    index: Int,
    total: Int,
    onJump: (Int) -> Unit,
    onFirst: () -> Unit,
    onLast: () -> Unit,
    onSheet: () -> Unit,
) {
    if (total <= 0) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JumpButton("⌂ 首页", Modifier.weight(1f)) { onFirst() }
            JumpButton("答题卡", Modifier.weight(1f)) { onSheet() }
            JumpButton("−100", Modifier.weight(1f)) { onJump(PracticeLogic.jump(index, -100, total)) }
            JumpButton("−10", Modifier.weight(1f)) { onJump(PracticeLogic.jump(index, -10, total)) }
            JumpButton("上一页", Modifier.weight(1f)) { onJump(PracticeLogic.jump(index, -1, total)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "第 ${index + 1} / $total 题",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f),
            )
            JumpButton("下一页", Modifier.weight(1f)) { onJump(PracticeLogic.jump(index, 1, total)) }
            JumpButton("+10", Modifier.weight(1f)) { onJump(PracticeLogic.jump(index, 10, total)) }
            JumpButton("+100", Modifier.weight(1f)) { onJump(PracticeLogic.jump(index, 100, total)) }
            JumpButton("尾页 ⇥", Modifier.weight(1f)) { onLast() }
        }
    }
}

@Composable
private fun JumpButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        modifier = modifier.heightIn(min = 34.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false) // ui-ok: 跳页按钮，固定短标签
    }
}
