/*
 *  Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 *  This file is part of QKSMS.
 *
 *  QKSMS is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  QKSMS is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.moez.QKSMS.feature.blocking.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.view.View
import androidx.core.view.isInvisible
import com.jakewharton.rxbinding2.view.clicks
import com.fulldive.extension.divesms.R
import com.moez.QKSMS.common.base.QkController
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.common.util.extensions.resolveThemeColor
import com.moez.QKSMS.injection.appComponent
import com.moez.QKSMS.util.Preferences
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import com.fulldive.extension.divesms.databinding.BlockingManagerControllerBinding
import javax.inject.Inject

class BlockingManagerController : QkController<BlockingManagerView, BlockingManagerState, BlockingManagerPresenter>(),
    BlockingManagerView {

    @Inject lateinit var colors: Colors
    @Inject override lateinit var presenter: BlockingManagerPresenter

    private lateinit var binding: BlockingManagerControllerBinding
    private val activityResumedSubject: PublishSubject<Unit> = PublishSubject.create()

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.blocking_manager_controller
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        binding = BlockingManagerControllerBinding.bind(view)
        presenter.bindIntents(this)
        setTitle(R.string.blocking_manager_title)
        showBackButton(true)

        val states = arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(-android.R.attr.state_activated))

        val textTertiary = view.context.resolveThemeColor(android.R.attr.textColorTertiary)
        val imageTintList = ColorStateList(states, intArrayOf(colors.theme().theme, textTertiary))

        binding.qksms.findViewById<View>(R.id.action)?.let { it as? android.widget.ImageView }?.imageTintList = imageTintList
        binding.callBlocker.findViewById<View>(R.id.action)?.let { it as? android.widget.ImageView }?.imageTintList = imageTintList
        binding.callControl.findViewById<View>(R.id.action)?.let { it as? android.widget.ImageView }?.imageTintList = imageTintList
        binding.shouldIAnswer.findViewById<View>(R.id.action)?.let { it as? android.widget.ImageView }?.imageTintList = imageTintList
    }

    override fun onActivityResumed(activity: Activity) {
        activityResumedSubject.onNext(Unit)
    }

    override fun render(state: BlockingManagerState) {
        binding.qksms.findViewById<View>(R.id.action)?.let { actionView ->
            (actionView as? android.widget.ImageView)?.setImageResource(getActionIcon(true))
            actionView.isActivated = true
            actionView.isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_QKSMS
        }

        binding.callBlocker.findViewById<View>(R.id.action)?.let { actionView ->
            (actionView as? android.widget.ImageView)?.setImageResource(getActionIcon(state.callBlockerInstalled))
            actionView.isActivated = state.callBlockerInstalled
            actionView.isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_CB && state.callBlockerInstalled
        }

        binding.callControl.findViewById<View>(R.id.action)?.let { actionView ->
            (actionView as? android.widget.ImageView)?.setImageResource(getActionIcon(state.callControlInstalled))
            actionView.isActivated = state.callControlInstalled
            actionView.isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_CC && state.callControlInstalled
        }

        binding.shouldIAnswer.findViewById<View>(R.id.action)?.let { actionView ->
            (actionView as? android.widget.ImageView)?.setImageResource(getActionIcon(state.siaInstalled))
            actionView.isActivated = state.siaInstalled
            actionView.isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_SIA && state.siaInstalled
        }
    }

    private fun getActionIcon(installed: Boolean): Int = when {
        !installed -> R.drawable.ic_chevron_right_black_24dp
        else -> R.drawable.ic_check_white_24dp
    }

    override fun activityResumed(): Observable<*> = activityResumedSubject
    override fun qksmsClicked(): Observable<*> = binding.qksms.clicks()
    override fun callBlockerClicked(): Observable<*> = binding.callBlocker.clicks()
    override fun callControlClicked(): Observable<*> = binding.callControl.clicks()
    override fun siaClicked(): Observable<*> = binding.shouldIAnswer.clicks()

    override fun showCopyDialog(manager: String): Single<Boolean> = Single.create { emitter ->
        AlertDialog.Builder(activity)
                .setTitle(R.string.blocking_manager_copy_title)
                .setMessage(resources?.getString(R.string.blocking_manager_copy_summary, manager))
                .setPositiveButton(R.string.button_continue) { _, _ -> emitter.onSuccess(true) }
                .setNegativeButton(R.string.button_cancel) { _, _ -> emitter.onSuccess(false) }
                .setCancelable(false)
                .show()
    }

}
