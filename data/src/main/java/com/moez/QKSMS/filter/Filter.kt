
package com.moez.QKSMS.filter

abstract class Filter<in T> {

    abstract fun filter(item: T, query: CharSequence): Boolean

}