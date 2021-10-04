package com.moez.QKSMS.appextension

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.moez.QKSMS.R

object InstallBrowserDialogBuilder {

	private var dialog: AlertDialog? = null

	fun show(context: Context, onPositiveClicked: () -> Unit) {
		val view = LayoutInflater.from(context)
			.inflate(R.layout.install_browser_dialog_layout, null)
		val dialog = AlertDialog
			.Builder(context)
			.setView(view)
			.setPositiveButton(R.string.install_submit) { _, _ ->
				onPositiveClicked.invoke()
			}
			.setNegativeButton(R.string.rate_cancel) { _, _ -> }
			.create()

		dialog.setOnShowListener {
			dialog.getButton(AlertDialog.BUTTON_POSITIVE)
				?.setTextColor(ContextCompat.getColor(context, R.color.textColorAccent))
			dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
				?.setTextColor(ContextCompat.getColor(context, R.color.textColorSecondary))
		}
		dialog.show()
	}

	private fun setButtonColor(buttonId: Int, color: Int) {
		dialog?.getButton(buttonId)?.setTextColor(color)
	}
}