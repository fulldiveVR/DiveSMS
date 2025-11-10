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

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.moez.QKSMS.common.util.extensions.setVisible
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import io.realm.OrderedRealmCollection
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.RealmRecyclerViewAdapter
import io.realm.RealmResults
import timber.log.Timber

abstract class QkRealmAdapter<T : RealmModel> : RealmRecyclerViewAdapter<T, QkViewHolder>(null, true) {

    /**
     * This view can be set, and the adapter will automatically control the visibility of this view
     * based on the data
     */
    var emptyView: View? = null
        set(value) {
            if (field === value) return

            field = value
            value?.setVisible(data?.isLoaded == true && data?.isEmpty() == true)
        }

    private val emptyListener: (OrderedRealmCollection<T>) -> Unit = { data ->
        emptyView?.setVisible(data.isLoaded && data.isEmpty())
    }

    val selectionChanges: Subject<List<Long>> = BehaviorSubject.create()

    private var selection = listOf<Long>()

    /**
     * Toggles the selected state for a particular view
     *
     * If we are currently in selection mode (we have an active selection), then the state will
     * toggle. If we are not in selection mode, then we will only toggle if [force]
     */
    protected fun toggleSelection(id: Long, force: Boolean = true): Boolean {
        if (!force && selection.isEmpty()) return false

        selection = when (selection.contains(id)) {
            true -> selection - id
            false -> selection + id
        }

        selectionChanges.onNext(selection)
        return true
    }

    protected fun isSelected(id: Long): Boolean {
        return selection.contains(id)
    }

    fun clearSelection() {
        selection = listOf()
        selectionChanges.onNext(selection)
        notifyDataSetChanged()
    }

    override fun getItem(index: Int): T? {
        if (index < 0) {
            Timber.w("Only indexes >= 0 are allowed. Input was: $index")
            return null
        }

        return super.getItem(index)
    }

    override fun updateData(data: OrderedRealmCollection<T>?) {
        val updateTimestamp = System.currentTimeMillis()
        
        if (getData() === data) {
            return
        }

        val oldData = getData()
        
        removeListener(getData())
        addListener(data)

        data?.run {
            emptyListener(this)
        }

        // CRITICAL FIX: Handle frozen collections specially to avoid IllegalStateException
        if (data != null && (data as? RealmResults<T>)?.isFrozen == true) {
            
            // Manually update the adapter data without calling super.updateData() which tries to add listeners
            try {
                // Get the current data to compare
                val oldData = getData()
                val oldSize = oldData?.size ?: 0
                val newSize = data.size
                
                // Set the new data using reflection
                val field = RealmRecyclerViewAdapter::class.java.getDeclaredField("adapterData")
                field.isAccessible = true
                field.set(this, data)
                
                // Notify adapter with proper change notifications for better UI updates
                if (oldData == null) {
                    // First time setting data
                    notifyItemRangeInserted(0, newSize)
                } else if (oldSize != newSize) {
                    // Size changed
                    notifyDataSetChanged()
                } else {
                    // Same size, just content might have changed
                    notifyItemRangeChanged(0, newSize)
                }
                
                
                // CRITICAL FIX: Additional fallback - force complete refresh for frozen data
                Handler(Looper.getMainLooper()).post {
                    notifyDataSetChanged()
                    
                    // EXTREME FIX: Force RecyclerView to completely refresh
                    Handler(Looper.getMainLooper()).postDelayed({
                        notifyDataSetChanged()
                    }, 100)
                }
            } catch (e: Exception) {
                // If reflection fails, try super but catch the exception
                try {
                    super.updateData(data)
                } catch (frozenException: IllegalStateException) {
                    // Force a complete refresh as fallback
                    notifyDataSetChanged()
                }
            }
        } else {
            super.updateData(data)
        }
        
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        val attachTimestamp = System.currentTimeMillis()
        
        super.onAttachedToRecyclerView(recyclerView)
        addListener(data)
        
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        val detachTimestamp = System.currentTimeMillis()
        
        super.onDetachedFromRecyclerView(recyclerView)
        removeListener(data)
        
    }

    private fun addListener(data: OrderedRealmCollection<T>?) {
        val addListenerTimestamp = System.currentTimeMillis()
        when (data) {
            is RealmResults<T> -> {
                // CRITICAL FIX: Don't add listeners to frozen collections
                if (data.isFrozen) {
                } else {
                    data.addChangeListener(emptyListener)
                }
            }
            is RealmList<T> -> {
                // CRITICAL FIX: Don't add listeners to frozen collections
                if (data.isFrozen) {
                } else {
                    data.addChangeListener(emptyListener)
                }
            }
            null -> {
            }
        }
    }

    private fun removeListener(data: OrderedRealmCollection<T>?) {
        val removeListenerTimestamp = System.currentTimeMillis()
        when (data) {
            is RealmResults<T> -> {
                // CRITICAL FIX: Don't try to remove listeners from frozen collections
                if (data.isFrozen) {
                } else {
                    data.removeChangeListener(emptyListener)
                }
            }
            is RealmList<T> -> {
                // CRITICAL FIX: Don't try to remove listeners from frozen collections
                if (data.isFrozen) {
                } else {
                    data.removeChangeListener(emptyListener)
                }
            }
            null -> {
            }
        }
    }

}