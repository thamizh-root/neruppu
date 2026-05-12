package org.havenapp.neruppu.ui.features.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import org.havenapp.neruppu.domain.repository.SensorRepository
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val _filter = MutableStateFlow("All")
    val filter: StateFlow<String> = _filter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val events: Flow<PagingData<Event>> = _filter.flatMapLatest { filter ->
        sensorRepository.getEvents(filter)
    }.cachedIn(viewModelScope)

    fun setFilter(filter: String) {
        _filter.value = filter
    }

    fun clearLogs(deleteFiles: Boolean) {
        viewModelScope.launch {
            sensorRepository.clearEvents(deleteFiles)
        }
    }
}
