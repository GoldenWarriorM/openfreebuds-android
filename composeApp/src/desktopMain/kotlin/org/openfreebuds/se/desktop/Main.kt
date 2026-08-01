package org.openfreebuds.se.desktop

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.openfreebuds.se.connection.SeController
import org.openfreebuds.se.connection.SimulatorConnection
import org.openfreebuds.se.ui.App

fun main() = application {
    val scope = rememberCoroutineScope()

    val controller = remember {
        val demo = System.getenv("OPENFREEBUDS_DEMO") == "1"
        if (demo) {
            val sim = SimulatorConnection(scope)
            SeController(sim, sim, scope)
        } else {
            val bluez = BlueZConnection(scope)
            SeController(bluez, bluez, scope)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "FreeBuds SE",
        state = rememberWindowState(width = 460.dp, height = 820.dp),
    ) {
        App(controller)
    }
}
