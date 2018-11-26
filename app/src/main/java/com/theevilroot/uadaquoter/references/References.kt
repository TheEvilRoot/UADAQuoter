package com.theevilroot.uadaquoter.references

interface References {
    fun isKeyValid(string: String): Boolean
    fun getPrefix(): String
    fun getPostfix(): String
}