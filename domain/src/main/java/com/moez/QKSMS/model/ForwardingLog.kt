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
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

open class ForwardingLog(
    @PrimaryKey var id: Long = 0,

    @Index var filterId: Long = 0,
    var filterName: String = "",

    // SMS data
    var sender: String = "",
    var messageBody: String = "",
    @Index var receivedAt: Long = 0,
    var simSlot: Int = -1,

    // Delivery
    var destinationEmail: String = "",
    var deliveryStatus: String = "PENDING",
    var deliveryMethod: String = "",
    var errorMessage: String? = null,
    var sentAt: Long? = null,
    var retryCount: Int = 0
) : RealmObject()
