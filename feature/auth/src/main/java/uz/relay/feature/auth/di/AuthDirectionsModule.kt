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

/**
 * Auth ekranlarining `Contract.Directions` interfeyslarini ularning `DirectionsImpl`'lariga bog'laydi.
 *
 * ViewModel faqat interfeysni ko'radi, navigatsiya tafsilotlarini (qaysi key, ResetTo yoki Push) bilmaydi -
 * testda Directions'ni oson fake bilan almashtirish mumkin. Directions'ni faqat ViewModel'lar ishlatadi
 * va ular holat saqlamaydi, shuning uchun ViewModelComponent scope'ida o'rnatilgan.
 * `@Binds` ishlatilgan, chunki `@Provides`'dan farqli ravishda qo'shimcha kod generatsiya qilmaydi.
 */
@Module
@InstallIn(ViewModelComponent::class)
internal interface AuthDirectionsModule {

    @Binds
    fun bindPhoneDirections(impl: PhoneDirectionsImpl): PhoneContract.Directions

    @Binds
    fun bindOtpDirections(impl: OtpDirectionsImpl): OtpContract.Directions

    @Binds
    fun bindProfileSetupDirections(impl: ProfileSetupDirectionsImpl): ProfileSetupContract.Directions
}
