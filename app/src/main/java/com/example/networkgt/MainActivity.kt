package com.example.networkgt

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.networkgt.model.Arret
import com.example.networkgt.model.Graph
import com.example.networkgt.model.Noeud
import com.example.networkgt.ui.theme.NetworkGTTheme
import com.example.networkgt.view.DrawableGraph
import com.example.networkgt.view.GraphView
import com.example.networkgt.viewmodel.EditMode
import com.example.networkgt.viewmodel.GraphViewModel
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private val viewModel: GraphViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetworkGTTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GraphViewModel) {
    val context = LocalContext.current
    val graph by viewModel.graph.collectAsState(initial = Graph())
    val currentMode by viewModel.editMode.collectAsState(initial = EditMode.AJOUT_OBJET)
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf(R.drawable.plan1) }
    
    // États pour les dialogues
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var showEditNodeDialog by remember { mutableStateOf(false) }
    var showAddEdgeDialog by remember { mutableStateOf(false) }
    var showEditEdgeDialog by remember { mutableStateOf(false) }
    
    var lastTouchPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var selectedNode by remember { mutableStateOf<Noeud?>(null) }
    var selectedEdge by remember { mutableStateOf<Arret?>(null) }
    var edgeNodes by remember { mutableStateOf<Pair<Noeud, Noeud>?>(null) }
    
    var labelText by remember { mutableStateOf("") }

    val updatedMode by rememberUpdatedState(currentMode)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Network THIO/GANON") },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reset_network)) },
                                onClick = { viewModel.resetNetwork(); menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save_network)) },
                                onClick = { viewModel.saveGraph(context); menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Save, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.load_network)) },
                                onClick = { viewModel.loadGraph(context); menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.CloudDownload, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.send_email)) },
                                onClick = { sendEmail(context, graph, selectedPlan); menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Email, null) }
                            )
                            
                            HorizontalDivider()
                            
                            DropdownMenuItem(
                                text = { Text("Plan 1") },
                                onClick = { selectedPlan = R.drawable.plan1; menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Map, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Plan 2") },
                                onClick = { selectedPlan = R.drawable.plan2; menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Map, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Plan 3") },
                                onClick = { selectedPlan = R.drawable.plan3; menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Map, null) }
                            )
                            
                            HorizontalDivider()
                            
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mode_add_node)) },
                                onClick = { viewModel.setEditMode(EditMode.AJOUT_OBJET); menuExpanded = false },
                                leadingIcon = { RadioButton(selected = currentMode == EditMode.AJOUT_OBJET, onClick = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mode_add_edge)) },
                                onClick = { viewModel.setEditMode(EditMode.AJOUT_CONNEXION); menuExpanded = false },
                                leadingIcon = { RadioButton(selected = currentMode == EditMode.AJOUT_CONNEXION, onClick = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mode_edit)) },
                                onClick = { viewModel.setEditMode(EditMode.MODIFICATION); menuExpanded = false },
                                leadingIcon = { RadioButton(selected = currentMode == EditMode.MODIFICATION, onClick = null) }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { ctx -> GraphView(ctx) },
                update = { view ->
                    view.setGraph(graph)
                    view.setMode(updatedMode)
                    view.setPlan(selectedPlan)
                    
                    view.onNodeMoved = { node, x, y ->
                        viewModel.moveNoeud(node, x, y)
                    }

                    view.onCourbureChanged = { arret, courbure ->
                        viewModel.updateCourbure(arret, courbure)
                    }
                    
                    view.onLongPress = { x, y, node, arret ->
                        if (node == null && arret == null) {
                            if (updatedMode == EditMode.AJOUT_OBJET) {
                                lastTouchPoint = x to y
                                labelText = ""
                                showAddNodeDialog = true
                            }
                        } else if (node != null) {
                            selectedNode = node
                            labelText = node.label
                            showEditNodeDialog = true
                        } else if (arret != null) {
                            selectedEdge = arret
                            labelText = arret.label
                            showEditEdgeDialog = true
                        }
                    }
                    view.onConnectionCreated = { start, end ->
                        if (updatedMode == EditMode.AJOUT_CONNEXION) {
                            edgeNodes = start to end
                            labelText = ""
                            showAddEdgeDialog = true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // Dialogues (Ajout, Modification Objet/Connexion)
            // ... (conservé comme précédemment)
            if (showAddNodeDialog) {
                AlertDialog(
                    onDismissRequest = { showAddNodeDialog = false },
                    title = { Text(stringResource(R.string.label_prompt)) },
                    text = { TextField(value = labelText, onValueChange = { labelText = it }) },
                    confirmButton = {
                        Button(onClick = {
                            val textToSave = labelText
                            lastTouchPoint?.let { (x, y) -> viewModel.addNoeud(x, y, textToSave) }
                            showAddNodeDialog = false
                        }) { Text(stringResource(R.string.ok)) }
                    }
                )
            }

            if (showEditNodeDialog && selectedNode != null) {
                AlertDialog(
                    onDismissRequest = { showEditNodeDialog = false },
                    title = { Text("Modifier l'objet") },
                    text = {
                        Column {
                            TextField(value = labelText, onValueChange = { labelText = it })
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { 
                                viewModel.removeNoeud(selectedNode!!)
                                showEditNodeDialog = false
                            }) { Text("Supprimer") }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.updateNoeud(selectedNode!!, labelText, selectedNode!!.color)
                            showEditNodeDialog = false
                        }) { Text(stringResource(R.string.ok)) }
                    }
                )
            }

            if (showEditEdgeDialog && selectedEdge != null) {
                AlertDialog(
                    onDismissRequest = { showEditEdgeDialog = false },
                    title = { Text("Modifier la connexion") },
                    text = {
                        Column {
                            TextField(value = labelText, onValueChange = { labelText = it })
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { 
                                viewModel.removeArret(selectedEdge!!)
                                showEditEdgeDialog = false
                            }) { Text("Supprimer la connexion") }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.updateArret(selectedEdge!!, labelText, selectedEdge!!.color, selectedEdge!!.epaisseur)
                            showEditEdgeDialog = false
                        }) { Text(stringResource(R.string.ok)) }
                    }
                )
            }

            if (showAddEdgeDialog) {
                AlertDialog(
                    onDismissRequest = { showAddEdgeDialog = false },
                    title = { Text("Étiquette de connexion") },
                    text = { TextField(value = labelText, onValueChange = { labelText = it }) },
                    confirmButton = {
                        Button(onClick = {
                            val textToSave = labelText
                            edgeNodes?.let { (start, end) -> viewModel.addArret(start, end, textToSave) }
                            showAddEdgeDialog = false 
                        }) { Text(stringResource(R.string.ok)) }
                    }
                )
            }
        }
    }
}

/**
 * Fonction pour capturer le réseau en image et l'envoyer par mail
 */
fun sendEmail(context: Context, graph: Graph, planResId: Int) {
    try {
        val plan = BitmapFactory.decodeResource(context.resources, planResId)
        val drawableGraph = DrawableGraph(graph, plan)
        
        // On génère le bitmap à la taille du plan original
        val bitmap = drawableGraph.toBitmap(plan.width, plan.height)
        
        // Enregistrement temporaire
        val imagesFolder = File(context.cacheDir, "images")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, "network_export.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        val contentUri: Uri = FileProvider.getUriForFile(context, "com.example.networkgt.fileprovider", file)

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_SUBJECT, "Export Réseau NetworkGT")
            putExtra(Intent.EXTRA_TEXT, "Voici une capture de mon réseau d'objets connectés.")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(emailIntent, "Envoyer le réseau par mail"))

    } catch (e: Exception) {
        e.printStackTrace()
    }
}