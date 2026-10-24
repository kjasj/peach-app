package video.peach.android

import org.junit.Assert.*
import org.junit.Test
import video.peach.android.player.GestureMachine

class GestureMachineTest {
    @Test fun longPressRestoresPreviousSpeed() {
        val machine = GestureMachine()
        machine.down(1.5f)
        assertTrue(machine.hold())
        assertEquals(1.5f, machine.release())
        assertEquals(GestureMachine.Mode.IDLE, machine.mode)
    }
    @Test fun dragCancelsLongPress() {
        val machine = GestureMachine()
        machine.down(1f)
        assertTrue(machine.drag(horizontal = true, left = false))
        assertFalse(machine.hold())
        assertEquals(GestureMachine.Mode.SEEK, machine.mode)
        assertNull(machine.release())
    }
    @Test fun brightnessAndVolumeLockDirection() {
        val machine = GestureMachine()
        machine.down(1f)
        machine.drag(horizontal = false, left = true)
        machine.drag(horizontal = true, left = false)
        assertEquals(GestureMachine.Mode.BRIGHTNESS, machine.mode)
        machine.release()
        machine.down(1f)
        machine.drag(horizontal = false, left = false)
        assertEquals(GestureMachine.Mode.VOLUME, machine.mode)
    }
    @Test fun moveDuringBoostDoesNotSeek() {
        val machine = GestureMachine()
        machine.down(1.25f); machine.hold()
        assertFalse(machine.drag(horizontal = true, left = true))
        assertEquals(1.25f, machine.release())
        assertNull(machine.release())
    }
}
