package com.tom.fourhourbody.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.FourHourBodyApp

@Composable
fun rememberContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) {
        (context.applicationContext as FourHourBodyApp).container
    }
}
