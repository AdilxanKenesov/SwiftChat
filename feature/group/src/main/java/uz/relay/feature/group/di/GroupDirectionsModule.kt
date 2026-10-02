package uz.relay.feature.group.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import uz.relay.feature.group.create.GroupCreateContract
import uz.relay.feature.group.create.GroupCreateDirectionsImpl
import uz.relay.feature.group.info.GroupInfoContract
import uz.relay.feature.group.info.GroupInfoDirectionsImpl

/**
 * Guruh ekranlarining `Directions` interfeyslarini ularning `DirectionsImpl` realizatsiyalariga bog'laydigan Hilt moduli.
 *
 * Directions faqat ViewModel'larga kerak va holatsiz — shuning uchun [ViewModelComponent] doirasida o'rnatiladi
 * (Singleton qilishga hojat yo'q). `@Binds` ishlatiladi, chunki bu shunchaki interfeys → realizatsiya bog'lanishi:
 * qo'shimcha factory kodi generatsiya qilinmaydi. Testlarda esa ViewModel'ga soxta (fake) Directions berish oson.
 */
@Module
@InstallIn(ViewModelComponent::class)
internal interface GroupDirectionsModule {

    @Binds
    fun bindGroupCreateDirections(impl: GroupCreateDirectionsImpl): GroupCreateContract.Directions

    @Binds
    fun bindGroupInfoDirections(impl: GroupInfoDirectionsImpl): GroupInfoContract.Directions
}
