package org.openfreebuds.se.ui

import androidx.compose.runtime.Composable
import org.openfreebuds.se.connection.SeController

@Composable
fun App(controller: SeController) {
    SeAppTheme {
        HomeScreen(controller)
    }
}
