package uz.relay.core.navigation.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.relay.core.navigation.AppNavigationDispatcher
import uz.relay.core.navigation.AppNavigationHandler
import uz.relay.core.navigation.AppNavigator

/**
 * Navigatsiya uchun Hilt moduli. `@Binds` — interfeysni implementatsiyaga bog'laydi (qo'shimcha kod
 * generatsiyasiz, `@Provides` dan arzonroq).
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface NavigationModule {

    // Ikkala interfeys ham bitta @Singleton dispatcher'ga bog'lanadi — ViewModel yuborgan buyruq UI'ga yetib boradi.
    @Binds
    fun bindAppNavigator(impl: AppNavigationDispatcher): AppNavigator

    @Binds
    fun bindAppNavigationHandler(impl: AppNavigationDispatcher): AppNavigationHandler
}
