package uz.relay.feature.profile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import uz.relay.feature.profile.edit.EditProfileContract
import uz.relay.feature.profile.edit.EditProfileDirectionsImpl
import uz.relay.feature.profile.me.MyProfileContract
import uz.relay.feature.profile.me.MyProfileDirectionsImpl
import uz.relay.feature.profile.user.UserProfileContract
import uz.relay.feature.profile.user.UserProfileDirectionsImpl

/** Directions faqat ViewModel'larga kerak va holatsiz — shuning uchun ViewModel doirasida. */
@Module
@InstallIn(ViewModelComponent::class)
internal interface ProfileDirectionsModule {

    @Binds
    fun bindMyProfileDirections(impl: MyProfileDirectionsImpl): MyProfileContract.Directions

    @Binds
    fun bindEditProfileDirections(impl: EditProfileDirectionsImpl): EditProfileContract.Directions

    @Binds
    fun bindUserProfileDirections(impl: UserProfileDirectionsImpl): UserProfileContract.Directions
}
