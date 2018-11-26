package com.theevilroot.uadaquoter.objects

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

class Quote(@SerializedName("id") var id: Int,
            @SerializedName("adder") var adder: String,
            @SerializedName("author") var author: String,
            @SerializedName("edited_by") var editedBy: String? = null,
            @SerializedName("edited_at") var editedAt: Long = -1,
            @SerializedName("quote") var quote: String,
            var cached: Boolean = false) {


    fun toJson():JsonObject {
        val obj = JsonObject()
        obj.addProperty("id", id)
        obj.addProperty("adder", adder)
        obj.addProperty("author", author)
        obj.addProperty("quote", quote)
        obj.addProperty("edited_by", if(editedBy == null) "null" else editedBy)
        obj.addProperty("edited_at", editedAt)
        return obj
    }

    fun makeCached():Quote {
        this.cached = true
        return this
    }

    companion object {
        fun fromJson(obj: JsonObject): Quote =
                Quote(
                        id = obj["id"].asInt,
                        adder = obj["adder"].asString,
                        author = obj["author"].asString,
                        quote = obj["quote"].asString,
                        editedBy = if (obj["edited_by"].asString == "null") null else obj["edited_by"].asString,
                        editedAt = obj["edited_at"].asLong)
    }


}