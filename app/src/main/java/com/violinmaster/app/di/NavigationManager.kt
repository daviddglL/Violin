package com.violinmaster.app.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages UI navigation state: current tab index and overlay display.
 *
 * Pure StateFlow-based manager with zero Android dependencies.
 * Separated from SessionManager per Single Responsibility Principle.
 */
@Singleton
class NavigationManager @Inject constructor() {

  private val _currentTab = MutableStateFlow(0)
  val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

  private val _currentOverlay = MutableStateFlow<String?>(null)
  val currentOverlay: StateFlow<String?> = _currentOverlay.asStateFlow()

  fun selectTab(index: Int) {
    _currentTab.value = index
  }

  fun showOverlay(overlay: String?) {
    _currentOverlay.value = overlay
  }
}
