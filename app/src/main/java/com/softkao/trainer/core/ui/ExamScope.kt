package com.softkao.trainer.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ExamId

/**
 * 全站唯一科目。
 *
 * 之前各页面用 `currentExamId.first()` 只取一次快照，导致在 A 页切换科目后，
 * B 页还停留在旧科目。这里统一改成订阅 DataStore 的 Flow：
 * 任何一处调用 `setCurrentExamId`，所有正在显示的页面立刻跟着切换。
 *
 * 两门考试的数据本身仍然是完全隔离的（题库、错题、掌握度各算各的）。
 */
@Composable
fun rememberCurrentExamId(): String {
    val examId by ServiceLocator.settingsRepository.currentExamId
        .collectAsStateWithLifecycle(initialValue = ExamId.SYSTEM_INTEGRATION.id)
    return examId
}
