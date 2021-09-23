
package com.moez.QKSMS.common.util

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.moez.QKSMS.BuildConfig
import com.moez.QKSMS.manager.AnalyticsManager
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    context: Context,
    private val analyticsManager: AnalyticsManager
) : PurchasesUpdatedListener {

    companion object {
        const val SKU_PLUS = "remove_ads"
        const val SKU_PLUS_DONATE = "qksms_plus_donate"
    }

    val products: Observable<List<SkuDetails>> = BehaviorSubject.create()
    val upgradeStatus: Observable<Boolean>

    private val skus = listOf(SKU_PLUS, SKU_PLUS_DONATE)
    private val purchaseListObservable = BehaviorSubject.create<List<Purchase>>()

    private val billingClient: BillingClient = BillingClient.newBuilder(context).setListener(this).build()
    private var isServiceConnected = false

    init {
        startServiceConnection {
            queryPurchases()
            querySkuDetailsAsync()
        }

        upgradeStatus = when (BuildConfig.FLAVOR) {
            "noAnalytics" -> BehaviorSubject.createDefault(true)

            else -> purchaseListObservable
                    .map { purchases -> purchases.any { it.sku == SKU_PLUS } || purchases.any { it.sku == SKU_PLUS_DONATE } }
                    .doOnNext { upgraded -> analyticsManager.setUserProperty("Upgraded", upgraded) }
        }
    }

    private fun queryPurchases() {
        executeServiceRequest {
            // Load the cached data
            purchaseListObservable.onNext(billingClient.queryPurchases(SkuType.INAPP).purchasesList.orEmpty())

            // On a fresh device, the purchase might not be cached, and so we'll need to force a refresh
            billingClient.queryPurchaseHistoryAsync(SkuType.INAPP) { _, _ ->
                purchaseListObservable.onNext(billingClient.queryPurchases(SkuType.INAPP).purchasesList.orEmpty())
            }
        }
    }


    private fun startServiceConnection(onSuccess: () -> Unit) {
        val listener = object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingResponse billingResponseCode: Int) {
                if (billingResponseCode == BillingResponse.OK) {
                    isServiceConnected = true
                    onSuccess()
                } else {
                    Timber.w("Billing response: $billingResponseCode")
                    purchaseListObservable.onNext(listOf())
                }
            }

            override fun onBillingServiceDisconnected() {
                isServiceConnected = false
            }
        }

        Flowable.fromCallable { billingClient.startConnection(listener) }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    private fun querySkuDetailsAsync() {
        executeServiceRequest {
            val subParams = SkuDetailsParams.newBuilder().setSkusList(skus).setType(BillingClient.SkuType.INAPP)
            billingClient.querySkuDetailsAsync(subParams.build()) { responseCode, skuDetailsList ->
                if (responseCode == BillingResponse.OK) {
                    (products as Subject).onNext(skuDetailsList)
                }
            }
        }
    }

    fun initiatePurchaseFlow(activity: Activity, sku: String) {
        executeServiceRequest {
            val params = BillingFlowParams.newBuilder().setSku(sku).setType(SkuType.INAPP)
            billingClient.launchBillingFlow(activity, params.build())
        }
    }

    private fun executeServiceRequest(runnable: () -> Unit) {
        when (isServiceConnected) {
            true -> runnable()
            false -> startServiceConnection(runnable)
        }
    }

    override fun onPurchasesUpdated(resultCode: Int, purchases: List<Purchase>?) {
        if (resultCode == BillingResponse.OK) {
            purchaseListObservable.onNext(purchases.orEmpty())
        }
    }

}