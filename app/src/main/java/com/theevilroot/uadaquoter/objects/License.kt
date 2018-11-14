package com.theevilroot.uadaquoter.objects

import android.net.Uri

data class License(val title: String,
                   val licenseType: String,
                   val license: String,
                   val uri: Uri)