
package com.moez.QKSMS.feature.changelog

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.moez.QKSMS.BuildConfig
import com.moez.QKSMS.R
import com.moez.QKSMS.feature.main.MainActivity
import com.moez.QKSMS.manager.ChangelogManager
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.changelog_dialog.view.*

class ChangelogDialog(activity: MainActivity) {

    val moreClicks: Subject<Unit> = PublishSubject.create()

    private val dialog: AlertDialog
    private val adapter = ChangelogAdapter(activity)

    init {
        val layout = LayoutInflater.from(activity).inflate(R.layout.changelog_dialog, null)

        dialog = AlertDialog.Builder(activity)
                .setCancelable(true)
                .setView(layout)
                .create()

        layout.version.text = activity.getString(R.string.changelog_version, BuildConfig.VERSION_NAME)
        layout.more.setOnClickListener { dialog.dismiss(); moreClicks.onNext(Unit) }
        layout.dismiss.setOnClickListener { dialog.dismiss() }
    }

    fun show(changelog: ChangelogManager.Changelog) {
        adapter.setChangelog(changelog)
        dialog.show()
    }

}
