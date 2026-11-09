package com.personal.forest.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Process

fun hasUsageAccess(context: Context): Boolean {
    val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
    return ops.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName,
    ) == AppOpsManager.MODE_ALLOWED
}
