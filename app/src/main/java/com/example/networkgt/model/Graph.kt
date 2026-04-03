package com.example.networkgt.model

import java.io.Serializable

data class Graph(
    val noeuds: MutableList<Noeud> = mutableListOf(),
    val arrets: MutableList<Arret> = mutableListOf()
) : Serializable {
    fun addNoeud(noeud: Noeud) {
        noeuds.add(noeud)
    }

    fun addArret(arret: Arret) {
        if (!arretExistante(arret.debut, arret.fin) && arret.debut != arret.fin) {
            arrets.add(arret)
        }
    }

    fun removeNoeud(noeud: Noeud) {
        noeuds.remove(noeud)
        arrets.removeIf { it.debut == noeud || it.fin == noeud }
    }

    fun removeArret(arret: Arret) {
        arrets.remove(arret)
    }

    fun arretExistante(n1: Noeud, n2: Noeud): Boolean {
        return arrets.any { (it.debut == n1 && it.fin == n2) || (it.debut == n2 && it.fin == n1) }
    }

    fun clear() {
        noeuds.clear()
        arrets.clear()
    }
}