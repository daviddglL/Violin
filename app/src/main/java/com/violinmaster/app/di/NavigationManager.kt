package com.violinmaster.app.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages UI navigation state: current tab index, overlay display,
 * and deep link handling.
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

  private val _targetLessonsSubTab = MutableStateFlow(-1)
  val targetLessonsSubTab: StateFlow<Int> = _targetLessonsSubTab.asStateFlow()

  /** Deep link target: assignment ID to open in chat/assignments. */
  private val _deepLinkTargetAssignmentId = MutableStateFlow<String?>(null)
  val deepLinkTargetAssignmentId: StateFlow<String?> = _deepLinkTargetAssignmentId.asStateFlow()

  fun selectTab(index: Int) {
    _currentTab.value = index
  }

  fun showOverlay(overlay: String?) {
    _currentOverlay.value = overlay
  }

  fun navigateToQuizTab() {
    _currentTab.value = 1
    _targetLessonsSubTab.value = 2
  }

  fun clearLessonsSubTabTarget() {
    _targetLessonsSubTab.value = -1
  }

  /**
   * Navigates to a screen from a deep link or push notification.
   *
   * Supported targets:
   * - "home" → tab 0
   * - "lessons" → tab 1
   * - "stats" → tab 2
   * - "settings" → tab 3
   * - "assignment/{id}" → tab 1 + sets deepLinkTargetAssignmentId
   * - "chat/{id}" → tab 1 + sets deepLinkTargetAssignmentId
   * - "tuner" → overlay "tuner"
   * - "metronome" → overlay "metronome"
   *
   * @param screen The target screen identifier.
   * @param targetId Optional target ID (e.g., assignment ID).
   */
  fun navigateToDeepLink(screen: String?, targetId: String?) {
    when (screen) {
      "home" -> {
        _currentTab.value = 0
        _currentOverlay.value = null
      }
      "lessons" -> {
        _currentTab.value = 1
        _currentOverlay.value = null
      }
      "stats" -> {
        _currentTab.value = 2
        _currentOverlay.value = null
      }
      "settings" -> {
        _currentTab.value = 3
        _currentOverlay.value = null
      }
      "assignment" -> {
        _currentTab.value = 1 // Lessons tab contains assignments
        _currentOverlay.value = null
        _deepLinkTargetAssignmentId.value = targetId
      }
      "chat" -> {
        _currentTab.value = 1
        _currentOverlay.value = null
        _deepLinkTargetAssignmentId.value = targetId
      }
      "tuner" -> {
        _currentOverlay.value = "tuner"
      }
      "metronome" -> {
        _currentOverlay.value = "metronome"
      }
    }
  }

  /** Consumes the deep link target ID after it's been used for navigation. */
  fun consumeDeepLinkTarget() {
    _deepLinkTargetAssignmentId.value = null
  }
}
