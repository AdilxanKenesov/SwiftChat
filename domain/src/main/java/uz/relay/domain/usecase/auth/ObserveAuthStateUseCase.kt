package uz.relay.domain.usecase.auth

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.AuthState
import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Avtorizatsiya holatini ([AuthState]) kuzatadi. MainViewModel shu orqali qaysi ekrandan boshlashni hal qiladi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveAuthStateUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Flow<AuthState> = repository.authState
}
