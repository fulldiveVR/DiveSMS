
package com.moez.QKSMS.manager

import io.reactivex.Single

interface ChangelogManager {

    data class Changelog(
        val added: List<String>,
        val improved: List<String>,
        val fixed: List<String>
    )

    /**
     * Returns true if the app has benn updated since the last time this method was called
     */
    fun didUpdate(): Boolean

    fun getChangelog(): Single<Changelog>

    fun markChangelogSeen()

}
