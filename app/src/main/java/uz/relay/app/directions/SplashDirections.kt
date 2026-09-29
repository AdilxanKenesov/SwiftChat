package uz.relay.app.directions

import uz.relay.core.navigation.AppNavigator
import uz.relay.feature.auth.splash.SplashContract
import javax.inject.Inject

class SplashDirections @Inject constructor(
    private val navigator: AppNavigator
) : SplashContract.Directions
