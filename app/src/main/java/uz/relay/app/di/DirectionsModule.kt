package uz.relay.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import uz.relay.app.directions.SplashDirections
import uz.relay.feature.auth.splash.SplashContract

@Module
@InstallIn(ViewModelComponent::class)
interface DirectionsModule {

    @Binds
    fun bindSplashDirections(impl: SplashDirections): SplashContract.Directions
}
