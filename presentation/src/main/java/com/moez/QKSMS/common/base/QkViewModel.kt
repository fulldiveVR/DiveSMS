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

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

abstract class QkViewModel<in View : QkView<State>, State : Any>(initialState: State) : ViewModel() {

    protected val disposables = CompositeDisposable()
    protected val state: Subject<State> = BehaviorSubject.createDefault(initialState)

    private val stateReducer: Subject<State.() -> State> = PublishSubject.create()

    init {
        // If we accidentally push a realm object into the state on the wrong thread, switching
        // to mainThread right here should immediately alert us of the issue
        disposables.add(stateReducer
                .observeOn(AndroidSchedulers.mainThread())
                .scan(initialState) { state, reducer ->
                   reducer(state)
                }
               .subscribe { newState ->
                   state.onNext(newState)
               })
    }

    @CallSuper
    open fun bindView(view: View) {
        disposables.add(state
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { newState ->
                    val renderTimestamp = System.currentTimeMillis()
                    try {
                        view.render(newState)
                    } catch (e: Exception) {
                    }
                })
        
        // Immediately render current state if available
        if (state is BehaviorSubject) {
            (state as BehaviorSubject<State>).value?.let { currentState ->
                val initialRenderTimestamp = System.currentTimeMillis()
                try {
                    view.render(currentState)
                } catch (e: Exception) {
                }
            }
        }
    }

    protected fun newState(reducer: State.() -> State) {
        val updateTimestamp = System.currentTimeMillis()
        try {
            stateReducer.onNext(reducer)
        } catch (e: Exception) {
        }
    }

    override fun onCleared() = disposables.dispose()

}