package com.x8bit.bitwarden.ui.platform.feature.settings.vault.passwordhealth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the password health screen.
 */
@Serializable
data object PasswordHealthRoute

/**
 * Add Password Health destinations to the nav graph.
 */
fun NavGraphBuilder.passwordHealthDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<PasswordHealthRoute> {
        PasswordHealthScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the Password Health screen.
 */
fun NavController.navigateToPasswordHealth(navOptions: NavOptions? = null) {
    this.navigate(route = PasswordHealthRoute, navOptions = navOptions)
}
