package uz.relay.feature.calls.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import uz.relay.feature.calls.call.CallContract
import uz.relay.feature.calls.call.CallDirectionsImpl

/** Directions faqat ViewModel'larga kerak va holatsiz — shuning uchun ViewModel doirasida (@Binds). */
@Module
@InstallIn(ViewModelComponent::class)
internal interface CallsDirectionsModule {

    @Binds
    fun bindCallDirections(impl: CallDirectionsImpl): CallContract.Directions
}
