package uz.relay.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

typealias AppNavigationParam = NavBackStack<NavKey>.() -> Unit
