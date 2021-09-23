
package com.moez.QKSMS.feature.gallery

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.moez.QKSMS.injection.ViewModelKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Named

@Module
class GalleryActivityModule {

    @Provides
    fun provideIntent(activity: GalleryActivity): Intent = activity.intent

    @Provides
    @Named("partId")
    fun providePartId(activity: GalleryActivity): Long = activity.partId

    @Provides
    @IntoMap
    @ViewModelKey(GalleryViewModel::class)
    fun provideGalleryViewModel(viewModel: GalleryViewModel): ViewModel = viewModel

}