package com.durham.myapplication

import org.json.JSONException
import org.json.JSONObject

class CurrentScale {
    var note: String = "C"
    var type: String = "Ionian"
    var metronome: Boolean = false
    var tempo:Int = 44


    //JSON Constructor

    @Throws(JSONException::class) constructor(jo: JSONObject) {
        note = jo.getString("note")
        type = jo.getString("type")
        metronome = jo.getString("metronome").toBoolean()
        tempo = jo.getString("tempo").toInt()
    }
    //empty default constructor
    constructor() {

    }

    @Throws(JSONException::class) fun parseJSON(): JSONObject {
        val jo = JSONObject()
        jo.put("note", note)
        jo.put("type", type)
        jo.put("metronome", metronome.toString())
        jo.put("tempo", tempo.toString())
        return jo
    }
}