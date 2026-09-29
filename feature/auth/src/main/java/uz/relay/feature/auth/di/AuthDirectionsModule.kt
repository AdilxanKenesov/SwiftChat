package uz.relay.feature.auth.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import uz.relay.feature.auth.otp.OtpContract
import uz.relay.feature.auth.otp.OtpDirectionsImpl
import uz.relay.feature.auth.phone.PhoneContract
import uz.relay.feature.auth.phone.PhoneDirectionsImpl
import uz.relay.feature.auth.profile.ProfileSetupContract
import uz.relay.feature.auth.profile.ProfileSetupDirectionsImpl
import uz.relay.feature.auth.splash.SplashContract
import uz.relay.feature.auth.splash.SplashDirectionsImpl

/** Directions are only needed by ViewModels and hold no state, so they are ViewModel-scoped. */
@Module
@InstallIn(ViewModelComponent::class)
internal interface AuthDirectionsModule {

    @Binds
    fun bindSplashDirections(impl: SplashDirectionsImpl): SplashContract.Directions

    @Binds
    fun bindPhoneDirections(impl: PhoneDirectionsImpl): PhoneContract.Directions

    @Binds
    fun bindOtpDirections(impl: OtpDirectionsImpl): OtpContract.Directions

    @Binds
    fun bindProfileSetupDirections(impl: ProfileSetupDirectionsImpl): ProfileSetupContract.Directions
}
