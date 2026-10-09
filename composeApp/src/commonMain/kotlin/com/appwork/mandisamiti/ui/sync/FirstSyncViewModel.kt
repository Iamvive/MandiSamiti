package com.appwork.mandisamiti.ui.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FirstSyncState {
    data class Downloading(val count: Int) : FirstSyncState
    data object Done : FirstSyncState
    data object Failed : FirstSyncState
}

class FirstSyncViewModel(
    private val shopId: String,
    private val pull: suspend (String, (Int) -> Unit) -> Result<Int>,
    private val viewModelScope: CoroutineScope,
) {
    private val _state = MutableStateFlow<FirstSyncState>(FirstSyncState.Downloading(0))
    val state: StateFlow<FirstSyncState> = _state.asStateFlow()

    init { start() }

    fun retry() = start()

    private fun start() {
        _state.value = FirstSyncState.Downloading(0)
        viewModelScope.launch {
            val r = pull(shopId) { n -> _state.value = FirstSyncState.Downloading(n) }
            _state.value = if (r.isSuccess) FirstSyncState.Done else FirstSyncState.Failed
        }
    }
}
