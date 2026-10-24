package com.softkao.trainer

import android.app.Application

/**
 * 测试专用的 Application。
 *
 * Robolectric 跑测试时会自动创建真正的 SoftKaoApp，它会在后台线程把种子题库灌进 SQLite；
 * 而 Robolectric 的 SQLite 影子不支持跨线程访问，会抛
 * 「Illegal connection pointer」并污染后续所有测试。
 *
 * 真机上 SQLiteDatabase 本身是线程安全的，所以这只是测试环境的问题，
 * 这里换一个不初始化 ServiceLocator 的 Application 即可。
 */
class TestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 刻意不调用 ServiceLocator.init / seedIfNeeded
    }
}
