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


package com.moez.QKSMS.common.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.bluelinelabs.conductor.archlifecycle.LifecycleController
import com.fulldive.extension.divesms.R
import com.fulldive.extension.divesms.databinding.ToolbarBinding

abstract class QkController<ViewContract : QkViewContract<State>, State : Any, Presenter : QkPresenter<ViewContract, State>> : LifecycleController() {

    abstract var presenter: Presenter

    private val appCompatActivity: AppCompatActivity?
        get() = activity as? AppCompatActivity

    protected val themedActivity: QkThemedActivity?
        get() = activity as? QkThemedActivity

    protected var containerView: View? = null
    private var toolbarBinding: ToolbarBinding? = null

    @LayoutRes
    var layoutRes: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        return inflater.inflate(layoutRes, container, false).also {
            containerView = it
            // Try to find toolbar in the view
            val toolbar = it.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            if (toolbar != null) {
                toolbarBinding = ToolbarBinding.bind(toolbar)
            }
            onViewCreated()
        }
    }

    open fun onViewCreated() {
    }

    fun setTitle(@StringRes titleId: Int) {
        setTitle(activity?.getString(titleId))
    }

    fun setTitle(title: CharSequence?) {
        activity?.title = title
        toolbarBinding?.toolbarTitle?.text = title
    }

    fun showBackButton(show: Boolean) {
        appCompatActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(show)
    }

    override fun onDestroyView(view: View) {
        containerView = null
        toolbarBinding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onCleared()
    }

}
