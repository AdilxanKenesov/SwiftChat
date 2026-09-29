package uz.relay.core.navigation.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.relay.core.navigation.AppNavigationDispatcher
import uz.relay.core.navigation.AppNavigationHandler
import uz.relay.core.navigation.AppNavigator

@Module
@InstallIn(SingletonComponent::class)
internal interface NavigationModule {

    // Both interfaces bind to the one @Singleton dispatcher, so commands reach the UI.
    @Binds
    fun bindAppNavigator(impl: AppNavigationDispatcher): AppNavigator

    @Binds
    fun bindAppNavigationHandler(impl: AppNavigationDispatcher): AppNavigationHandler
}
