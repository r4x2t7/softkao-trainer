# -*- coding: utf-8 -*-
"""把按页面分作用域的科目选择，统一改成全局唯一的科目。

- SettingsRepository.examId(scope) -> currentExamId
- SettingsRepository.setExamId(scope, id) -> setCurrentExamId(id)
- 各页面不再有「不限科目」选项，两门考试彻底隔离
"""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "java" / "com" / "softkao" / "trainer"

REPLACEMENTS = [
    (r"ServiceLocator\.settingsRepository\.examId\(ExamScope\.\w+\)\.first\(\)",
     "ServiceLocator.settingsRepository.currentExamId.first()"),
    (r"ServiceLocator\.settingsRepository\.examId\(ExamScope\.\w+\)",
     "ServiceLocator.settingsRepository.currentExamId"),
    (r"ServiceLocator\.settingsRepository\.setExamId\(\s*ExamScope\.\w+,\s*",
     "ServiceLocator.settingsRepository.setCurrentExamId("),
    (r"settings\.examId\(ExamScope\.\w+\)\.first\(\)",
     "settings.currentExamId.first()"),
    (r"settings\.setExamId\(\s*ExamScope\.\w+,\s*",
     "settings.setCurrentExamId("),
    (r"ServiceLocator\.settingsRepository\.setExamId\(\s*ExamScope\.\w+,\s*",
     "ServiceLocator.settingsRepository.setCurrentExamId("),
]

changed = []
for path in ROOT.rglob("*.kt"):
    text = original = path.read_text(encoding="utf-8")
    for pat, rep in REPLACEMENTS:
        text = re.sub(pat, rep, text)
    # 去掉 ExamScope 的 import（若仍在使用则保留）
    if "ExamScope." not in text:
        text = text.replace("import com.softkao.trainer.core.model.ExamScope\n", "")
    if text != original:
        path.write_text(text, encoding="utf-8")
        changed.append(path.name)

print("已改写：", ", ".join(changed) if changed else "无")
