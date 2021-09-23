
package com.moez.QKSMS.mapper

interface Mapper<in From, out To> {

    fun map(from: From): To

}