package com.papra.mobile.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.papra.mobile.PapraApp
import com.papra.mobile.ui.ViewModelFactory
import com.papra.mobile.ui.auth.AuthViewModel
import com.papra.mobile.ui.auth.LoginScreen
import com.papra.mobile.ui.document.DocumentDetailScreen
import com.papra.mobile.ui.document.DocumentDetailViewModel
import com.papra.mobile.ui.home.HomeScreen
import com.papra.mobile.ui.home.HomeViewModel
import com.papra.mobile.ui.search.SearchScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val SEARCH = "search"
    const val DOCUMENT_DETAIL = "document/{orgId}/{documentId}"
    fun documentDetail(orgId: String, documentId: String) = "document/$orgId/$documentId"
}

@Composable
fun PapraNavGraph(app: PapraApp, navController: NavHostController = rememberNavController()) {
    // Single shared HomeViewModel instance so Search can reuse its org/doc state.
    val homeViewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory { HomeViewModel(app.documentRepository, app.sessionStore) },
    )

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            androidx.compose.animation.slideInHorizontally(initialOffsetX = { it / 4 }) + androidx.compose.animation.fadeIn()
        },
        exitTransition = {
            androidx.compose.animation.fadeOut(targetAlpha = 0.4f)
        },
        popEnterTransition = {
            androidx.compose.animation.fadeIn()
        },
        popExitTransition = {
            androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it / 4 }) + androidx.compose.animation.fadeOut()
        },
    ) {
        composable(Routes.SPLASH) {
            LaunchedEffect(Unit) {
                val hasSession = app.sessionStore.currentAuthMode() != com.papra.mobile.data.local.AuthMode.NONE &&
                    app.sessionStore.currentServerUrl() != null
                val destination = if (hasSession) Routes.HOME else Routes.LOGIN
                navController.navigate(destination) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
        composable(Routes.LOGIN) {
            val authViewModel: AuthViewModel = viewModel(
                factory = ViewModelFactory { AuthViewModel(app.authRepository) },
            )
            LoginScreen(
                viewModel = authViewModel,
                onSignedIn = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onOpenDocument = { doc ->
                    navController.navigate(Routes.documentDetail(doc.organizationId, doc.id))
                },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                homeViewModel = homeViewModel,
                onOpenDocument = { doc ->
                    navController.navigate(Routes.documentDetail(doc.organizationId, doc.id))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.DOCUMENT_DETAIL,
            arguments = listOf(
                navArgument("orgId") { type = NavType.StringType },
                navArgument("documentId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val orgId = backStackEntry.arguments?.getString("orgId").orEmpty()
            val documentId = backStackEntry.arguments?.getString("documentId").orEmpty()
            val detailViewModel: DocumentDetailViewModel = viewModel(
                factory = ViewModelFactory { DocumentDetailViewModel(app.documentRepository, app.sessionStore, orgId) },
            )
            DocumentDetailScreen(
                documentId = documentId,
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
