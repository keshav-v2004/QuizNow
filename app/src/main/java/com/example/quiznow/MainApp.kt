package com.example.quiznow



import QuizViewModel
import QuizViewModelFactory
import UserManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.quiznow.screens.BookmarkedQuestionsScreen
import com.example.quiznow.screens.HomeScreen
import com.example.quiznow.screens.LoginScreen
import com.example.quiznow.screens.PastQuizzesScreen
import com.example.quiznow.screens.QuizScreen
import com.example.quiznow.screens.QuizSetupScreen
import com.example.quiznow.screens.ResultScreen
import com.example.quiznow.screens.SignUpScreen
import com.example.quiznow.viewModels.AuthViewModel

// Main App Composable
@Composable
fun QuizApp(context: Context) {

    val navController = rememberNavController()
    val factory = remember { QuizViewModelFactory(context.applicationContext) }
    val viewModel: QuizViewModel = viewModel(factory = factory)

    val userManager = remember { UserManager(context) }
    val authViewModel: AuthViewModel = remember { AuthViewModel(userManager , viewModel) }

    val startDestination = if (userManager.isUserLoggedIn()) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(navController, authViewModel, userManager)
        }
        composable("signup") {
            SignUpScreen(navController, authViewModel, userManager)
        }
        composable("home") {
            HomeScreen(navController, viewModel , authViewModel = authViewModel , userManager = userManager )
        }
        composable("quiz_setup/{topic}") { backStackEntry ->
            val topic = backStackEntry.arguments?.getString("topic") ?: ""
            QuizSetupScreen(navController, viewModel, topic)
        }
        composable("quiz/{topic}/{count}/{difficulty}") { backStackEntry ->
            val topic = backStackEntry.arguments?.getString("topic") ?: ""
            val count = backStackEntry.arguments?.getString("count")?.toIntOrNull() ?: 5
            val difficulty = backStackEntry.arguments?.getString("difficulty") ?: "medium"
            QuizScreen(navController, viewModel, topic, count, difficulty)
        }
        composable("result/{score}/{total}/{topic}/{difficulty}") { backStackEntry ->
            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
            val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 0
            val topic = backStackEntry.arguments?.getString("topic") ?: ""
            val difficulty = backStackEntry.arguments?.getString("difficulty") ?: ""
            ResultScreen(navController, score, total, topic, difficulty)
        }
        composable("bookmarks") {
            BookmarkedQuestionsScreen(navController, viewModel)
        }
        composable("past_quizzes") {
            PastQuizzesScreen(navController, viewModel)
        }
    }
}