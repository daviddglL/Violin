package com.violinmaster.app.domain.model

/**
 * Represents a musical instrument supported by the app.
 *
 * Each instrument carries a localized label key and the open-string
 * definitions that drive pitch detection, reference tone playback,
 * and UI string selectors.
 *
 * @param labelKey Localization key for display (e.g., "instrument_violin").
 * @param strings Open strings from lowest to highest pitch.
 */
enum class Instrument(
  val labelKey: String,
  val strings: List<InstrumentString>
) {
  VIOLIN(
    labelKey = "instrument_violin",
    strings = listOf(
      InstrumentString(name = "G", frequency = 196.0),
      InstrumentString(name = "D", frequency = 293.66),
      InstrumentString(name = "A", frequency = 440.0),
      InstrumentString(name = "E", frequency = 659.25)
    )
  ),
  VIOLA(
    labelKey = "instrument_viola",
    strings = listOf(
      InstrumentString(name = "C", frequency = 130.8),
      InstrumentString(name = "G", frequency = 196.0),
      InstrumentString(name = "D", frequency = 293.66),
      InstrumentString(name = "A", frequency = 440.0)
    )
  ),
  CELLO(
    labelKey = "instrument_cello",
    strings = listOf(
      InstrumentString(name = "C", frequency = 65.4),
      InstrumentString(name = "G", frequency = 98.0),
      InstrumentString(name = "D", frequency = 146.8),
      InstrumentString(name = "A", frequency = 220.0)
    )
  ),
  DOUBLE_BASS(
    labelKey = "instrument_double_bass",
    strings = listOf(
      InstrumentString(name = "E", frequency = 41.2),
      InstrumentString(name = "A", frequency = 55.0),
      InstrumentString(name = "D", frequency = 73.4),
      InstrumentString(name = "G", frequency = 98.0)
    )
  )
}

/**
 * An open (unfingered) string on an instrument.
 *
 * @param name Note letter only (G, D, A, E, C). Octave disambiguation
 *             is applied at display time by the UI layer.
 * @param frequency Frequency in Hz at A4 = 440 reference pitch.
 */
data class InstrumentString(
  val name: String,
  val frequency: Double
)
