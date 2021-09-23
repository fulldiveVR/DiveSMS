
package com.moez.QKSMS.feature.main

import android.content.Context
import com.moez.QKSMS.experiment.Experiment
import com.moez.QKSMS.experiment.Variant
import com.moez.QKSMS.manager.AnalyticsManager
import javax.inject.Inject

class DrawerBadgesExperiment @Inject constructor(
    context: Context,
    analyticsManager: AnalyticsManager
) : Experiment<Boolean>(context, analyticsManager) {

    override val key: String = "Drawer Badges"

    override val variants: List<Variant<Boolean>> = listOf(
            Variant("variant_a", false),
            Variant("variant_b", true))

    override val default: Boolean = false

    override val qualifies: Boolean = true

}