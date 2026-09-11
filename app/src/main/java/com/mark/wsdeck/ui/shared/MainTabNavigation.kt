package com.mark.wsdeck.ui.shared

import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination

/** 主分頁明確回到自己的根頁，不還原與首頁共用的牌組詳情堆疊。 */
fun NavHostController.navigateToMainTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = false }
        launchSingleTop = true
        restoreState = false
    }
}
