package com.example.wakemeup

import com.example.wakemeup.alarm.ActiveAlarmStore
import com.example.wakemeup.model.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AlarmStoreTest {

    @Test
    fun testSetActiveAlarmUpdatesStore() {
        val destination = Destination(
            title = "Test Destination",
            address = "456 Test Ave",
            latitude = 40.7128,
            longitude = -74.0060
        )

        ActiveAlarmStore.setActiveAlarm(destination, 1500f)

        assertEquals(destination, ActiveAlarmStore.activeDestination.value)
        assertEquals(1500f, ActiveAlarmStore.radiusMeters.value, 0.01f)
        assertFalse(ActiveAlarmStore.isAlarmTriggered.value)
    }

    @Test
    fun testResetActiveAlarm() {
        val destination = Destination(
            title = "Test Destination",
            address = "456 Test Ave",
            latitude = 40.7128,
            longitude = -74.0060
        )

        ActiveAlarmStore.setActiveAlarm(destination, 2000f)
        assertEquals(destination, ActiveAlarmStore.activeDestination.value)
    }
}
