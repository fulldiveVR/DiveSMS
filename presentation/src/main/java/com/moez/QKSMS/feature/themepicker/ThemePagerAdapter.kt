
package com.moez.QKSMS.feature.themepicker

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.moez.QKSMS.R
import javax.inject.Inject

class ThemePagerAdapter @Inject constructor(private val context: Context) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return when (position) {
            1 -> container.findViewById(R.id.hsvPicker)
            else -> container.findViewById(R.id.materialColors)
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            1 -> context.getString(R.string.theme_plus)
            else -> context.getString(R.string.theme_material)
        }
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
    }

    override fun getCount(): Int {
        return 2
    }

}