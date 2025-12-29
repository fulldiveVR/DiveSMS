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

open class ForwardingFilter(
    @PrimaryKey var id: Long = 0,

    // Basic info
    var name: String = "",
    var enabled: Boolean = true,
    var priority: Int = 0,
    var createdAt: Long = 0,
    var updatedAt: Long = 0,

    // Filter conditions
    var senderFilter: String = "",
    var senderMatchType: String = "CONTAINS",
    var contentFilter: String = "",
    var contentMatchType: String = "CONTAINS",
    var isIncludeFilter: Boolean = true,

    // Time window
    var timeWindowEnabled: Boolean = false,
    var startTime: String? = null,
    var endTime: String? = null,
    var activeDays: String = "",

    // SIM card
    var simSlotFilter: String = "ALL",

    // Destination
    var destinationEmail: String = "",
    var emailAccountId: Long = 0,

    // Template
    var emailSubjectTemplate: String = "SMS from {{contact_name}}",
    var emailBodyTemplate: String = "",
    var useHtmlTemplate: Boolean = false,

    // Stats
    var forwardedCount: Int = 0,
    var lastTriggered: Long? = null
) : RealmObject()
