package org.havenapp.neruppu.ui.features.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.repository.SensorRepository

class LogsViewModel(
    private val sensorRepository: SensorRepository
) : ViewModel() {

    val events: Flow<PagingData<Event>> = sensorRepository.getEvents()
        .cachedIn(viewModelScope)

    fun clearLogs() {
        viewModelScope.launch {
            sensorRepository.clearEvents()
        }
    }
}
