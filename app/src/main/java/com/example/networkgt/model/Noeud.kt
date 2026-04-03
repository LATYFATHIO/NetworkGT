package com.example.networkgt.model

import android.graphics.Color
import java.io.Serializable

data class Noeud(
    var id: String = java.util.UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var label: String,
    var color: Int = Color.BLUE,
    var iconResId: Int? = null
) : Serializable