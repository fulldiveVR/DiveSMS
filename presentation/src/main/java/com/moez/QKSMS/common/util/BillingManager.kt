package com.moez.QKSMS.common.util

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor() {
    val upgradeStatus: Observable<Boolean> = BehaviorSubject.createDefault(true)
}