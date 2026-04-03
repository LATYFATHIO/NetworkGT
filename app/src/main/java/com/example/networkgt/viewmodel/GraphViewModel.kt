package com.example.networkgt.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkgt.model.Arret
import com.example.networkgt.model.Graph
import com.example.networkgt.model.Noeud
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

enum class EditMode { AJOUT_OBJET, AJOUT_CONNEXION, MODIFICATION }

class GraphViewModel : ViewModel() {
    private val _graph = MutableStateFlow(Graph())
    val graph: StateFlow<Graph> = _graph.asStateFlow()

    private val _editMode = MutableStateFlow(EditMode.AJOUT_OBJET)
    val editMode: StateFlow<EditMode> = _editMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val FILE_NAME = "network_graph.ser"

    fun setEditMode(mode: EditMode) { _editMode.value = mode }

    fun moveNoeud(noeud: Noeud, newX: Float, newY: Float) {
        val currentGraph = _graph.value
        
        val updatedNoeuds = currentGraph.noeuds.map {
            if (it.id == noeud.id) it.copy(x = newX, y = newY) else it
        }.toMutableList()
        
        val movedNode = updatedNoeuds.find { it.id == noeud.id } ?: return

        val updatedArrets = currentGraph.arrets.map { arret ->
            when {
                arret.debut.id == movedNode.id -> arret.copy(debut = movedNode)
                arret.fin.id == movedNode.id -> arret.copy(fin = movedNode)
                else -> arret
            }
        }.toMutableList()

        _graph.value = currentGraph.copy(noeuds = updatedNoeuds, arrets = updatedArrets)
    }

    fun addNoeud(x: Float, y: Float, label: String) {
        val currentGraph = _graph.value
        val newList = currentGraph.noeuds.toMutableList()
        newList.add(Noeud(x = x, y = y, label = label))
        _graph.value = currentGraph.copy(noeuds = newList)
    }

    fun addArret(debut: Noeud, fin: Noeud, label: String) {
        val currentGraph = _graph.value
        if (debut.id == fin.id) return
        val dejaConnectes = currentGraph.arrets.any {
            (it.debut.id == debut.id && it.fin.id == fin.id) ||
            (it.debut.id == fin.id && it.fin.id == debut.id)
        }
        if (!dejaConnectes) {
            val newList = currentGraph.arrets.toMutableList()
            newList.add(Arret(debut = debut, fin = fin, label = label))
            _graph.value = currentGraph.copy(arrets = newList)
        }
    }

    fun removeNoeud(noeud: Noeud) {
        val currentGraph = _graph.value
        val newList = currentGraph.noeuds.toMutableList()
        newList.removeIf { it.id == noeud.id }
        val newArrets = currentGraph.arrets.toMutableList()
        newArrets.removeIf { it.debut.id == noeud.id || it.fin.id == noeud.id }
        _graph.value = currentGraph.copy(noeuds = newList, arrets = newArrets)
    }

    fun removeArret(arret: Arret) {
        val currentGraph = _graph.value
        val newList = currentGraph.arrets.toMutableList()
        newList.removeIf { it.id == arret.id }
        _graph.value = currentGraph.copy(arrets = newList)
    }

    fun updateNoeud(noeud: Noeud, label: String, color: Int) {
        val currentGraph = _graph.value
        val updatedNoeuds = currentGraph.noeuds.map {
            if (it.id == noeud.id) it.copy(label = label, color = color) else it
        }.toMutableList()
        
        val updatedArrets = currentGraph.arrets.map { arret ->
            val newDebut = updatedNoeuds.find { it.id == arret.debut.id } ?: arret.debut
            val newFin = updatedNoeuds.find { it.id == arret.fin.id } ?: arret.fin
            arret.copy(debut = newDebut, fin = newFin)
        }.toMutableList()

        _graph.value = currentGraph.copy(noeuds = updatedNoeuds, arrets = updatedArrets)
    }

    fun updateArret(arret: Arret, label: String, color: Int, epaisseur: Float) {
        val currentGraph = _graph.value
        val updatedArrets = currentGraph.arrets.map {
            if (it.id == arret.id) it.copy(label = label, color = color, epaisseur = epaisseur) else it
        }.toMutableList()
        _graph.value = currentGraph.copy(arrets = updatedArrets)
    }

    fun updateCourbure(arret: Arret, courbure: Float) {
        val currentGraph = _graph.value
        val updatedArrets = currentGraph.arrets.map {
            if (it.id == arret.id) it.copy(courbure = courbure) else it
        }.toMutableList()
        _graph.value = currentGraph.copy(arrets = updatedArrets)
    }

    fun resetNetwork() { _graph.value = Graph() }

    fun saveGraph(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                try {
                    delay(300)
                    context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use { fos ->
                        ObjectOutputStream(fos).use { oos -> oos.writeObject(_graph.value) }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            _isLoading.value = false
        }
    }

    fun loadGraph(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                try {
                    delay(300)
                    context.openFileInput(FILE_NAME).use { fis ->
                        ObjectInputStream(fis).use { ois ->
                            val loadedGraph = ois.readObject() as Graph
                            _graph.value = loadedGraph
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            _isLoading.value = false
        }
    }
}