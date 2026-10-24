package com.softkao.trainer

import android.app.Application
import android.content.Context
import com.softkao.trainer.core.data.SettingsRepository
import com.softkao.trainer.core.data.MaterialRepository
import com.softkao.trainer.core.data.QuestionBankImporter
import com.softkao.trainer.core.data.SeedImporter
import com.softkao.trainer.core.data.db.AppDatabase
import com.softkao.trainer.core.data.remote.AiExplainer
import com.softkao.trainer.core.data.remote.BankUpdater
import com.softkao.trainer.core.data.repo.PracticeRepository
import com.softkao.trainer.core.data.repo.QuestionRepository
import com.softkao.trainer.core.data.repo.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SoftKaoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        ServiceLocator.seedIfNeeded()
    }
}

/** 极简依赖容器：规模上来后再换 Hilt，替换点只有这里。 */
object ServiceLocator {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var db: AppDatabase
        private set
    lateinit var questionRepository: QuestionRepository
        private set
    lateinit var practiceRepository: PracticeRepository
        private set
    lateinit var statsRepository: StatsRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var materialRepository: MaterialRepository
        private set
    lateinit var importer: QuestionBankImporter
        private set
    lateinit var seedImporter: SeedImporter
        private set
    lateinit var bankUpdater: BankUpdater
        private set
    lateinit var aiExplainer: AiExplainer
        private set

    fun init(context: Context) {
        val app = context.applicationContext
        db = AppDatabase(app)
        questionRepository = QuestionRepository(db)
        practiceRepository = PracticeRepository(db)
        statsRepository = StatsRepository(db)
        settingsRepository = SettingsRepository(app)
        materialRepository = MaterialRepository(app)
        importer = QuestionBankImporter(db)
        seedImporter = SeedImporter(app, db, importer)
        bankUpdater = BankUpdater(db, importer)
        aiExplainer = AiExplainer()
    }

    /** 首次启动把内置种子题库导入本地；已有数据则跳过 */
    fun seedIfNeeded() {
        appScope.launch {
            if (db.questionCount() > 0) return@launch
            seedImporter.importAll()
        }
    }
}
