package com.theevilroot.uadaquoter.utils.exceptions

class APIException(val reason: String): Exception("QuoterApi error: $reason")