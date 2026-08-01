package org.openfreebuds.se.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openfreebuds.se.connection.SeController
import org.openfreebuds.se.model.BatteryLevels
import org.openfreebuds.se.model.SeDevice
import kotlin.test.Test
import kotlin.test.assertNotNull

class RfcommIntegrationTest {

    @Test
    fun connectAndReadBattery() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val conn = BlueZConnection(scope)
        conn.refresh()

        var device: SeDevice? = null
        val start = System.currentTimeMillis()
        while (device == null && System.currentTimeMillis() - start < 10_000) {
            device = conn.devices.value.firstOrNull()
            delay(200)
        }
        assertNotNull(device, "no FreeBuds SE device found")
        println("DEVICE: ${device!!.address} ${device!!.name}")

        val controller = SeController(conn, conn, scope)
        var battery: BatteryLevels? = null
        controller.onBatteryChanged = {
            battery = it
            println("BATTERY: L=${it.left} R=${it.right} C=${it.case}")
        }

        controller.connect(device!!)
        val start2 = System.currentTimeMillis()
        while (battery == null && System.currentTimeMillis() - start2 < 15_000) {
            delay(300)
        }
        println("FINAL STATE: ${conn.state.value}")
        assertNotNull(battery, "battery never arrived; state=${conn.state.value}")
        conn.close()
    }
}
