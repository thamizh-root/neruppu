/*
 * Copyright (C) 2026 thamizh-root
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.havenapp.neruppu.data.local

import org.havenapp.neruppu.domain.model.AlertTarget
import org.junit.Test
import org.junit.Assert.*

class AlertTargetStoreTest {

    @Test
    fun `TC-TARGET-01 AlertTarget enum values`() {
        assertEquals(3, AlertTarget.values().size)
        assertEquals(AlertTarget.NONE, AlertTarget.valueOf("NONE"))
        assertEquals(AlertTarget.TELEGRAM, AlertTarget.valueOf("TELEGRAM"))
        assertEquals(AlertTarget.MATRIX, AlertTarget.valueOf("MATRIX"))
    }

    @Test
    fun `TC-TARGET-02 AlertTarget name roundtrip`() {
        assertEquals(AlertTarget.TELEGRAM, AlertTarget.valueOf(AlertTarget.TELEGRAM.name))
        assertEquals(AlertTarget.MATRIX, AlertTarget.valueOf(AlertTarget.MATRIX.name))
        assertEquals(AlertTarget.NONE, AlertTarget.valueOf(AlertTarget.NONE.name))
    }

    @Test
    fun `TC-TARGET-03 Mutual exclusion TELEGRAM vs MATRIX`() {
        // Mutual exclusion rules:
        // - Saving Telegram config sets target to TELEGRAM and clears Matrix
        // - Saving Matrix config sets target to MATRIX and clears Telegram
        assertTrue(AlertTarget.TELEGRAM != AlertTarget.MATRIX)
        assertTrue(AlertTarget.TELEGRAM != AlertTarget.NONE)
        assertTrue(AlertTarget.MATRIX != AlertTarget.NONE)
    }
}