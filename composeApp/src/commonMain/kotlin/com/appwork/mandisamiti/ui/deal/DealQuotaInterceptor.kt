package com.appwork.mandisamiti.ui.deal

import com.appwork.mandisamiti.data.repository.QuotaRepository
import com.appwork.mandisamiti.data.repository.ShopQuota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

sealed interface DealSaveState {
    data object Idle : DealSaveState
    data object Saving : DealSaveState
    data class Success(val billId: String? = null) : DealSaveState
    data class EmergencyExtensionPrompt(val quota: ShopQuota) : DealSaveState
    data class QuotaExhaustedUpgradeRequired(val quota: ShopQuota) : DealSaveState
    data class Error(val message: String) : DealSaveState
}

class DealQuotaInterceptor(
    private val quotaRepository: QuotaRepository
) {
    private val _saveState = MutableStateFlow<DealSaveState>(DealSaveState.Idle)
    val saveState: StateFlow<DealSaveState> = _saveState.asStateFlow()

    suspend fun checkQuotaAndExecuteSave(
        shopId: String,
        isEditMode: Boolean = false,
        onSave: suspend () -> Unit
    ): DealSaveState {
        // Edit mode never consumes additional quota
        if (isEditMode) {
            _saveState.value = DealSaveState.Saving
            return try {
                onSave()
                DealSaveState.Success().also { _saveState.value = it }
            } catch (e: Throwable) {
                DealSaveState.Error(e.message ?: "Failed to save deal").also { _saveState.value = it }
            }
        }

        val quota = quotaRepository.getQuotaStatus(shopId).first()
        if (quota.canCreateBill) {
            _saveState.value = DealSaveState.Saving
            return try {
                onSave()
                quotaRepository.incrementLocalBillCount(shopId)
                DealSaveState.Success().also { _saveState.value = it }
            } catch (e: Throwable) {
                DealSaveState.Error(e.message ?: "Failed to save deal").also { _saveState.value = it }
            }
        } else {
            return if (quota.canClaimEmergencyExtension) {
                DealSaveState.EmergencyExtensionPrompt(quota).also { _saveState.value = it }
            } else {
                DealSaveState.QuotaExhaustedUpgradeRequired(quota).also { _saveState.value = it }
            }
        }
    }

    suspend fun claimEmergencyAndSave(
        shopId: String,
        onSave: suspend () -> Unit
    ): DealSaveState {
        _saveState.value = DealSaveState.Saving
        val claimResult = quotaRepository.claimEmergencyExtension(shopId)
        return if (claimResult.isSuccess) {
            try {
                onSave()
                quotaRepository.incrementLocalBillCount(shopId)
                DealSaveState.Success().also { _saveState.value = it }
            } catch (e: Throwable) {
                DealSaveState.Error(e.message ?: "Failed to save deal").also { _saveState.value = it }
            }
        } else {
            val err = claimResult.exceptionOrNull()?.message ?: "Failed to claim emergency extension"
            DealSaveState.Error(err).also { _saveState.value = it }
        }
    }

    fun resetState() {
        _saveState.value = DealSaveState.Idle
    }
}
