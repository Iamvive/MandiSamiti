package com.appwork.mandisamiti.ui.sync

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FirstSyncViewModelTest {
    @Test
    fun reportsProgressThenDone() = runTest {
        val vm = FirstSyncViewModel("shop-1", { _, progress -> progress(120); progress(240); Result.success(240) }, backgroundScope)
        assertEquals(FirstSyncState.Done, vm.state.first { it == FirstSyncState.Done })
    }

    @Test
    fun failureShowsFailedAndRetryRunsAgain() = runTest {
        var calls = 0
        val vm = FirstSyncViewModel("shop-1", { _, _ -> if (++calls == 1) Result.failure(RuntimeException()) else Result.success(0) }, backgroundScope)
        vm.state.first { it == FirstSyncState.Failed }
        vm.retry()
        vm.state.first { it == FirstSyncState.Done }
        assertEquals(2, calls)
    }
}
