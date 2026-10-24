package com.softkao.trainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.softkao.trainer.feature.bank.BankScreen
import com.softkao.trainer.feature.home.HomeScreen
import com.softkao.trainer.feature.knowledge.ChapterDetailScreen
import com.softkao.trainer.feature.knowledge.KnowledgeScreen
import com.softkao.trainer.feature.practice.PracticeScreen
import com.softkao.trainer.feature.search.SearchScreen
import com.softkao.trainer.feature.stats.StatsScreen
import com.softkao.trainer.feature.wrong.WrongScreen
import com.softkao.trainer.ui.theme.SoftKaoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoftKaoTheme {
                AppRoot()
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val WRONG = "wrong"
    const val STATS = "stats"
    const val BANK = "bank"
    const val SEARCH = "search"
    const val KNOWLEDGE = "knowledge"
    const val KNOWLEDGE_CHAPTER = "knowledge/chapter/{chapter}"
    const val PRACTICE =
        "practice?examId={examId}&mode={mode}&chapter={chapter}&kp={kp}&title={title}&ids={ids}"
}

private data class Tab(val route: String, val label: String, val glyph: String)

private val tabs = listOf(
    Tab(Routes.HOME, "首页", "🏠"),
    Tab(Routes.KNOWLEDGE, "知识点", "📚"),
    Tab(Routes.WRONG, "错题本", "📕"),
    Tab(Routes.STATS, "统计", "📊"),
    Tab(Routes.BANK, "设置", "⚙️"),
)

@Composable
fun AppRoot(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = { Text(tab.glyph) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onStartPractice = { examId, mode, chapter, kp, title ->
                        navController.navigate(
                            "practice?examId=$examId&mode=${mode.name}&chapter=$chapter&kp=$kp&title=$title"
                        )
                    },
                    onOpenWrong = { navController.navigate(Routes.WRONG) },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onOpenBank = { navController.navigate(Routes.BANK) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenQuestion = { questionId, examId ->
                        navController.navigate(
                            "practice?examId=$examId&mode=WRONG&chapter=&kp=&title=%E6%90%9C%E7%B4%A2%E7%BB%93%E6%9E%9C&ids=$questionId"
                        )
                    },
                )
            }
            composable(Routes.KNOWLEDGE) {
                KnowledgeScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChapter = { chapter ->
                        navController.navigate("knowledge/chapter/$chapter")
                    },
                    onPracticeChapter = { examId, chapter ->
                        navController.navigate(
                            "practice?examId=$examId&mode=CHAPTER&chapter=$chapter&kp=&title=" +
                                java.net.URLEncoder.encode("章节 · $chapter", "UTF-8")
                        )
                    },
                )
            }
            composable(
                route = Routes.KNOWLEDGE_CHAPTER,
                arguments = listOf(navArgument("chapter") { type = NavType.StringType }),
            ) { entry ->
                val chapter = entry.arguments?.getString("chapter").orEmpty()
                ChapterDetailScreen(
                    chapter = chapter,
                    onBack = { navController.popBackStack() },
                    onPracticeChapter = { e, c ->
                        navController.navigate(
                            "practice?examId=$e&mode=CHAPTER&chapter=$c&kp=&title=" +
                                java.net.URLEncoder.encode("章节 · $c", "UTF-8")
                        )
                    },
                )
            }
            composable(Routes.WRONG) {
                WrongScreen(
                    onStartPractice = { examId, mode, ids ->
                        navController.navigate(
                            "practice?examId=$examId&mode=${mode.name}&chapter=&kp=&ids=$ids"
                        )
                    },
                )
            }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.BANK) { BankScreen() }
            composable(
                route = "practice?examId={examId}&mode={mode}&chapter={chapter}&kp={kp}&title={title}&ids={ids}",
                arguments = listOf(
                    navArgument("examId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("mode") { type = NavType.StringType; defaultValue = "SEQUENTIAL" },
                    navArgument("chapter") { type = NavType.StringType; defaultValue = "" },
                    navArgument("kp") { type = NavType.StringType; defaultValue = "" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("ids") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val args = entry.arguments
                PracticeScreen(
                    examId = args?.getString("examId").orEmpty(),
                    mode = args?.getString("mode").orEmpty(),
                    chapter = args?.getString("chapter").orEmpty(),
                    knowledgePoint = args?.getString("kp").orEmpty(),
                    title = args?.getString("title").orEmpty(),
                    presetIds = args?.getString("ids").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
