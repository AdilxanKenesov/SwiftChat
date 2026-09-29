package uz.relay.feature.group.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import uz.relay.feature.group.create.GroupCreateContract
import uz.relay.feature.group.create.GroupCreateDirectionsImpl
import uz.relay.feature.group.info.GroupInfoContract
import uz.relay.feature.group.info.GroupInfoDirectionsImpl

/** Directions faqat ViewModel'larga kerak va holatsiz — shuning uchun ViewModel doirasida. */
@Module
@InstallIn(ViewModelComponent::class)
internal interface GroupDirectionsModule {

    @Binds
    fun bindGroupCreateDirections(impl: GroupCreateDirectionsImpl): GroupCreateContract.Directions

    @Binds
    fun bindGroupInfoDirections(impl: GroupInfoDirectionsImpl): GroupInfoContract.Directions
}
