package com.theevilroot.uadaquoter

import android.app.Activity
import android.support.annotation.IdRes
import android.view.View

fun <T: View> Activity.bind(@IdRes id: Int): Lazy<T> =
        lazy { findViewById<T>(id) }