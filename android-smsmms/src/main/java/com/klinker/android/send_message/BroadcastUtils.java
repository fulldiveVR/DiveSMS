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

package com.klinker.android.send_message;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Utility for helping with Android O changes. This means that we need to explicitly send broadcasts
 * to the correct receiver instead of implicitly. This is done by attaching the class and package
 * name to the intent based on the provided action.
 */
public class BroadcastUtils {

    public static void sendExplicitBroadcast(Context context, Intent intent, String action) {
        addClassName(context, intent, action);
        intent.setAction(action);
        context.sendBroadcast(intent);
    }

    public static void addClassName(Context context, Intent intent, String action) {
        PackageManager pm = context.getPackageManager();

        try {
            PackageInfo packageInfo =
                    pm.getPackageInfo(context.getPackageName(), PackageManager.GET_RECEIVERS);

            ActivityInfo[] receivers = packageInfo.receivers;
            for (ActivityInfo receiver : receivers) {
                if (receiver.taskAffinity.equals(action)) {
                    intent.setClassName(receiver.packageName, receiver.name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        intent.setPackage(context.getPackageName());
    }
}
