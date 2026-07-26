package com.dongnemarket.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * 앱의 화면 지도(뒤로가기 스택 포함)를 한 곳에서 관리한다.
 *
 * 각 `composable(route) { ... }` 블록이 화면 하나다. 아직 Unit 0(기반)이라
 * 내용은 [PlaceholderScreen] 이고, Unit 1~4 에서 실제 화면으로 교체된다.
 *
 * 화면은 NavController 를 직접 받지 않고 **람다(onXxx)** 로 이동 의도만 알린다.
 * 이렇게 해야 화면이 네비게이션을 모르는 순수한 UI 가 되고 Compose 테스트에서 단독 실행할 수 있다.
 */
@Composable
fun MarketOnNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = MarketOnRoutes.LOGIN,
    ) {
        // 로그인 — Unit 1
        composable(MarketOnRoutes.LOGIN) {
            PlaceholderScreen(
                title = "로그인",
                unit = "Unit 1",
                actionLabel = "홈으로",
                onAction = {
                    navController.navigate(MarketOnRoutes.HOME) {
                        // 로그인 성공 후 뒤로가기로 로그인 화면에 돌아오지 못하게 스택에서 제거
                        popUpTo(MarketOnRoutes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        // 홈 + 상품목록 — Unit 2
        composable(MarketOnRoutes.HOME) {
            PlaceholderScreen(
                title = "홈 · 상품목록",
                unit = "Unit 2",
                actionLabel = "상품 상세로(id=1)",
                onAction = { navController.navigate(MarketOnRoutes.productDetail(1L)) },
                secondaryLabel = "채팅 목록으로",
                onSecondary = { navController.navigate(MarketOnRoutes.CHAT_LIST) },
            )
        }

        // 상품 상세 — Unit 3
        composable(
            route = MarketOnRoutes.PRODUCT_DETAIL_PATTERN,
            arguments = listOf(navArgument(MarketOnRoutes.ARG_PRODUCT_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong(MarketOnRoutes.ARG_PRODUCT_ID) ?: 0L
            PlaceholderScreen(
                title = "상품 상세 (id=$productId)",
                unit = "Unit 3",
                actionLabel = "채팅방으로(roomId=1)",
                onAction = { navController.navigate(MarketOnRoutes.chatRoom(1L)) },
                secondaryLabel = "뒤로",
                onSecondary = { navController.popBackStack() },
            )
        }

        // 채팅 목록 — Unit 4
        composable(MarketOnRoutes.CHAT_LIST) {
            PlaceholderScreen(
                title = "채팅 목록",
                unit = "Unit 4",
                actionLabel = "채팅방으로(roomId=1)",
                onAction = { navController.navigate(MarketOnRoutes.chatRoom(1L)) },
                secondaryLabel = "뒤로",
                onSecondary = { navController.popBackStack() },
            )
        }

        // 채팅방 — Unit 4
        composable(
            route = MarketOnRoutes.CHAT_ROOM_PATTERN,
            arguments = listOf(navArgument(MarketOnRoutes.ARG_ROOM_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getLong(MarketOnRoutes.ARG_ROOM_ID) ?: 0L
            PlaceholderScreen(
                title = "채팅방 (roomId=$roomId)",
                unit = "Unit 4",
                secondaryLabel = "뒤로",
                onSecondary = { navController.popBackStack() },
            )
        }
    }
}
