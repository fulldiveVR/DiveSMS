/*
 *  Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 *  This file is part of QKSMS.
 *
 *  QKSMS is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  QKSMS is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.moez.QKSMS.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class EmailAccount(
    @PrimaryKey var id: Long = 0,

    var accountType: String = "GMAIL",
    var email: String = "",

    // Gmail OAuth (tokens stored in encrypted prefs, not Realm)
    var gmailAccountName: String? = null,

    // SMTP config
    var smtpHost: String? = null,
    var smtpPort: Int = 587,
    var smtpUsername: String? = null,
    var smtpUseTls: Boolean = true,

    var isDefault: Boolean = false,
    var createdAt: Long = 0,
    var lastUsed: Long? = null
) : RealmObject()
