
package com.moez.QKSMS.feature.blocking.numbers

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.moez.QKSMS.util.PhoneNumberUtils

class BlockedNumberTextWatcher(
    private val editText: EditText,
    private val phoneNumberUtils: PhoneNumberUtils
) : TextWatcher {

    init {
        editText.addTextChangedListener(this)
    }

    override fun afterTextChanged(s: Editable?) = Unit

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val formatted = s?.let(phoneNumberUtils::formatNumber)
        if (s?.toString() != formatted && formatted != null) {
            editText.setText(formatted)
            editText.setSelection(formatted.length)
        }
    }

    fun dispose() {
        editText.removeTextChangedListener(this)
    }

}
