package uz.relay.feature.calls.call

import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import javax.inject.Inject

internal class CallDirectionsImpl @Inject constructor(
    private val navigator: AppNavigator
) : CallContract.Directions {

    override suspend fun back() = navigator.navigate(AppNavigationParam.Back)
}
