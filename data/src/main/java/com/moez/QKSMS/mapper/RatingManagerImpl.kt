
package com.moez.QKSMS.mapper

import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.moez.QKSMS.manager.AnalyticsManager
import com.moez.QKSMS.manager.RatingManager
import io.reactivex.rxkotlin.Observables
import javax.inject.Inject

class RatingManagerImpl @Inject constructor(
    rxPrefs: RxSharedPreferences,
    private val analyticsManager: AnalyticsManager
) : RatingManager {

    companion object {
        private const val RATING_THRESHOLD = 10
    }

    private val sessions = rxPrefs.getInteger("sessions", 0)
    private val rated = rxPrefs.getBoolean("rated", false)
    private val dismissed = rxPrefs.getBoolean("dismissed", false)

    override val shouldShowRating = Observables.combineLatest(
            sessions.asObservable(),
            rated.asObservable(),
            dismissed.asObservable()
    ) { sessions, rated, dismissed ->
        sessions > RATING_THRESHOLD && !rated && !dismissed
    }

    override fun addSession() {
        sessions.set(sessions.get() + 1)
    }

    override fun rate() {
        analyticsManager.track("Clicked Rate")
        rated.set(true)
    }

    override fun dismiss() {
        analyticsManager.track("Clicked Rate (Dismiss)")
        dismissed.set(true)
    }

}
