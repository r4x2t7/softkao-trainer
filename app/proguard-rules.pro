# 默认 ProGuard 规则保持最小化；当前 release 未开启混淆（isMinifyEnabled = false）。
# 如需开启混淆，注意保留 kotlinx.serialization / Room 等反射相关规则。
-keepattributes *Annotation*
