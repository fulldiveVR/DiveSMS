
package com.moez.QKSMS.util

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.f2prateek.rx.preferences2.RxSharedPreferences
import java.io.InputStream

@GlideModule
class GlideAppModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setLogLevel(Log.ERROR)
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // TODO use DI to create the ContactImageLoader.Factory
        val rxPrefs = RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(context))
        val prefs = Preferences(context, rxPrefs)
        registry.append(String::class.java, InputStream::class.java, ContactImageLoader.Factory(context, prefs))
    }

}