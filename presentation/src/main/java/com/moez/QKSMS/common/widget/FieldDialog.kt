
package com.moez.QKSMS.common.widget

import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.moez.QKSMS.R
import kotlinx.android.synthetic.main.field_dialog.view.*

class FieldDialog(context: Activity, hint: String, listener: (String) -> Unit) : AlertDialog(context) {

    private val layout = LayoutInflater.from(context).inflate(R.layout.field_dialog, null)

    init {
        layout.field.hint = hint

        setView(layout)
        setButton(DialogInterface.BUTTON_NEUTRAL, context.getString(R.string.button_cancel)) { _, _ -> }
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.button_delete)) { _, _ -> listener("") }
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.button_save)) { _, _ ->
            listener(layout.field.text.toString())
        }
    }

    fun setText(text: String): FieldDialog {
        layout.field.setText(text)
        return this
    }

}
