package org.havenapp.neruppu.domain.repository

import org.havenapp.neruppu.domain.model.AlertTarget

interface AlertTargetRepository {
    val activeTarget: AlertTarget
    fun setActiveTarget(target: AlertTarget)
    fun clearActiveTarget()
}