
package com.moez.QKSMS.common.base

import androidx.lifecycle.LifecycleOwner

interface QkViewContract<in State>: LifecycleOwner {

    fun render(state: State)

}