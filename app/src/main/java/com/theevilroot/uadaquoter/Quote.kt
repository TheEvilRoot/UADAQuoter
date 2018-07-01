package com.theevilroot.uadaquoter

import com.google.gson.JsonObject

data class Quote(val id: Int, val adder: String, val author: String, val text: String, var cached: Boolean = false, var editedBy: String?, var editedAt: Long) {

    fun toJson():JsonObject {
        val obj = JsonObject()
        obj.addProperty("id", id)
        obj.addProperty("adder", adder)
        obj.addProperty("author", author)
        obj.addProperty("text", text)
        obj.addProperty("editedBy", editedBy)
        obj.addProperty("editedAt", editedAt ?: -1)
        return obj
    }
    companion object {
        fun fromJson(obj: JsonObject): Quote =
                Quote(obj["id"].asInt,
                        obj["adder"].asString,
                        obj["author"].asString,
                        obj["text"].asString,
                        editedBy = if(!obj.has("editedBy") || obj["editedBy"] == null || obj["editedBy"].isJsonNull) null else obj["editedBy"].asString,
                        editedAt = obj["editedAt"].asLong)
    }

}