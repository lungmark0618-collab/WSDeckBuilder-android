package com.mark.wsdeck.ui.shared

import android.content.Context
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainTabNavigationTest {
    @Test fun drawerDeckCanReturnDirectlyToEveryMainTab() {
        val nav = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        nav.navigatorProvider.addNavigator(ComposeNavigator())
        nav.graph = nav.createGraph(startDestination = "home") {
            composable("home") {}
            composable("catalog") {}
            composable("decks") {}
            composable("deck/{uuid}") {}
            composable("deck/{uuid}/cards") {}
        }
        nav.navigate("deck/example")
        nav.navigateToMainTab("home")
        assertEquals("home", nav.currentDestination?.route)
        repeat(2) {
            for (target in listOf("home", "catalog", "decks")) {
                nav.navigateToMainTab("decks")
                nav.navigate("deck/example")
                assertEquals("deck/{uuid}", nav.currentDestination?.route)
                nav.navigateToMainTab(target)
                assertEquals(target, nav.currentDestination?.route)
                nav.navigateToMainTab("decks")
                nav.navigate("deck/example")
                nav.navigate("deck/example/cards")
                nav.navigateToMainTab(target)
                assertEquals(target, nav.currentDestination?.route)
            }
        }
    }
}
