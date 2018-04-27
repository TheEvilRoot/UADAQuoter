package com.theevilroot.uadaquoter

data class Quote(val id: Int, val adder: String, val author: String, val text: String, val cached: Boolean = false)