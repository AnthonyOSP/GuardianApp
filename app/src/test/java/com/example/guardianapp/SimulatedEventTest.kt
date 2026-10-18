package com.example.guardianapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Texto recibido del ESP32 por BLE → evento conocido (ver SimulatedEvent.fromBlePayload). */
class SimulatedEventTest {

    @Test
    fun codigosConocidos_seReconocen() {
        assertEquals(SimulatedEvent.EMERGENCY, SimulatedEvent.fromBlePayload("EMERGENCY"))
        assertEquals(SimulatedEvent.FOOD, SimulatedEvent.fromBlePayload("FOOD"))
        assertEquals(SimulatedEvent.BATHROOM, SimulatedEvent.fromBlePayload("BATHROOM"))
        assertEquals(SimulatedEvent.HELP, SimulatedEvent.fromBlePayload("HELP"))
    }

    @Test
    fun ignoraMayusculasYEspacios() {
        assertEquals(SimulatedEvent.EMERGENCY, SimulatedEvent.fromBlePayload("  emergency\n"))
        assertEquals(SimulatedEvent.HELP, SimulatedEvent.fromBlePayload("Help"))
    }

    @Test
    fun textoDesconocido_daNull() {
        assertNull(SimulatedEvent.fromBlePayload("EVENTO_TEST"))
        assertNull(SimulatedEvent.fromBlePayload(""))
        assertNull(SimulatedEvent.fromBlePayload("EMERGENCIA"))
    }
}
