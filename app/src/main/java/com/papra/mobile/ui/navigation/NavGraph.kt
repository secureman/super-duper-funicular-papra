package com.papra.mobile.ui.navigation

import androidx.compose.runtime.Composable
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

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
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
                factory = ViewModelFactory { DocumentDetailViewModel(app.documentRepository, orgId) },
            )
            DocumentDetailScreen(
                documentId = documentId,
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
