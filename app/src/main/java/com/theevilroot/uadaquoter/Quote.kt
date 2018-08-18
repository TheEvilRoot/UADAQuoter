package com.theevilroot.uadaquoter

import com.google.gson.JsonObject

data class Quote(val id: Int, val adder: String, val author: String, val text: String, var cached: Boolean = false, var editedBy: String?, var editedAt: Long) {

    fun toJson():JsonObject {
        val obj = JsonObject()
        obj.addProperty("id", id)
        obj.addProperty("adder", adder)
        obj.addProperty("author", author)
        obj.addProperty("quote", text)
        obj.addProperty("edited_by", if(editedBy == null) "null" else editedBy)
        obj.addProperty("edited_at", editedAt)
        return obj
    }
    companion object {
        fun fromJson(obj: JsonObject): Quote =
                Quote(obj["id"].asInt,
                        obj["adder"].asString,
                        obj["author"].asString,
                        obj["quote"].asString,
                        editedBy = if(obj["edited_by"].asString == "null") null else obj["edited_by"].asString,
                        editedAt = obj["edited_at"].asLong)
    }

}