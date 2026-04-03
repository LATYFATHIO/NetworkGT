package com.example.networkgt.model

import android.graphics.Color
import java.io.Serializable

data class Arret(
    var id: String = java.util.UUID.randomUUID().toString(),
    var debut: Noeud,
    var fin: Noeud,
    var label: String,
    var color: Int = Color.BLACK,
    var epaisseur: Float = 5f,
    var courbure: Float = 0f // 0 means straight line
) : Serializable