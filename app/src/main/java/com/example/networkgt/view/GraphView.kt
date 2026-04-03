package com.example.networkgt.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.example.networkgt.R
import com.example.networkgt.model.Arret
import com.example.networkgt.model.Graph
import com.example.networkgt.model.Noeud
import com.example.networkgt.viewmodel.EditMode
import kotlin.math.*

class GraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var graph: Graph = Graph()
    private var currentMode: EditMode = EditMode.AJOUT_OBJET
    private var backgroundPlan: Bitmap? = null
    private var currentPlanResId: Int = R.drawable.plan1
    
    private var scaleFactor: Float = 1f
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    private val paintNode = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val paintEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private var draggingNode: Noeud? = null
    private var stretchingArret: Arret? = null
    private var connectionStartNode: Noeud? = null
    private var currentTouchPoint = PointF()

    var onLongPress: (Float, Float, Noeud?, Arret?) -> Unit = { _, _, _, _ -> }
    var onConnectionCreated: (Noeud, Noeud) -> Unit = { _, _ -> }
    var onNodeMoved: (Noeud, Float, Float) -> Unit = { _, _, _ -> }
    var onCourbureChanged: (Arret, Float) -> Unit = { _, _ -> }

    init {
        loadPlan(currentPlanResId)
    }

    private fun loadPlan(resId: Int) {
        try {
            backgroundPlan = BitmapFactory.decodeResource(resources, resId)
        } catch (e: Exception) {}
    }

    fun setPlan(resId: Int) {
        if (currentPlanResId != resId) {
            currentPlanResId = resId
            loadPlan(resId)
            invalidate()
        }
    }

    fun setGraph(newGraph: Graph) {
        this.graph = newGraph
        invalidate()
    }

    fun setMode(mode: EditMode) {
        this.currentMode = mode
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        backgroundPlan?.let { bitmap ->
            val scaleW = width.toFloat() / bitmap.width.toFloat()
            val scaleH = height.toFloat() / bitmap.height.toFloat()
            scaleFactor = min(scaleW, scaleH)

            offsetX = (width - bitmap.width * scaleFactor) / 2f
            offsetY = (height - bitmap.height * scaleFactor) / 2f
            
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scaleFactor, scaleFactor)

            canvas.drawBitmap(bitmap, 0f, 0f, null)

            graph.arrets.forEach { drawArret(canvas, it) }

            if (currentMode == EditMode.AJOUT_CONNEXION && connectionStartNode != null) {
                paintEdge.color = Color.GRAY
                paintEdge.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                canvas.drawLine(connectionStartNode!!.x, connectionStartNode!!.y, currentTouchPoint.x, currentTouchPoint.y, paintEdge)
                paintEdge.pathEffect = null
            }

            graph.noeuds.forEach { node ->
                paintNode.color = node.color
                val rect = RectF(node.x - 50f, node.y - 50f, node.x + 50f, node.y + 50f)
                canvas.drawRoundRect(rect, 20f, 20f, paintNode)
                canvas.drawText(node.label, node.x, node.y - 65f, paintText)
            }
            
            canvas.restore()
        }
    }

    private fun drawArret(canvas: Canvas, arret: Arret) {
        paintEdge.color = arret.color
        paintEdge.strokeWidth = arret.epaisseur
        val path = getArretPath(arret)
        canvas.drawPath(path, paintEdge)
        
        val pm = PathMeasure(path, false)
        val pos = FloatArray(2)
        pm.getPosTan(pm.length * 0.5f, pos, null)
        canvas.drawText(arret.label, pos[0], pos[1] - 20f, paintText)
    }

    private fun getArretPath(arret: Arret): Path {
        val path = Path()
        path.moveTo(arret.debut.x, arret.debut.y)
        if (arret.courbure == 0f) {
            path.lineTo(arret.fin.x, arret.fin.y)
        } else {
            val midX = (arret.debut.x + arret.fin.x) / 2
            val midY = (arret.debut.y + arret.fin.y) / 2
            val dx = arret.fin.x - arret.debut.x
            val dy = arret.fin.y - arret.debut.y
            val len = max(1f, sqrt(dx*dx + dy*dy))
            val nx = -dy / len
            val ny = dx / len
            val ctrlX = midX + nx * arret.courbure * 2
            val ctrlY = midY + ny * arret.courbure * 2
            path.quadTo(ctrlX, ctrlY, arret.fin.x, arret.fin.y)
        }
        return path
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            val ax = (e.x - offsetX) / scaleFactor
            val ay = (e.y - offsetY) / scaleFactor
            val node = findNodeAt(ax, ay)
            val arret = if (node == null) findArretAt(ax, ay) else null
            onLongPress(ax, ay, node, arret)
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        val x = (event.x - offsetX) / scaleFactor
        val y = (event.y - offsetY) / scaleFactor

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val node = findNodeAt(x, y)
                if (node != null) {
                    if (currentMode == EditMode.AJOUT_CONNEXION) {
                        connectionStartNode = node
                    } else if (currentMode == EditMode.MODIFICATION) {
                        draggingNode = node
                    }
                } else if (currentMode == EditMode.MODIFICATION) {
                    stretchingArret = findArretAt(x, y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggingNode != null) {
                    onNodeMoved(draggingNode!!, x, y)
                    invalidate()
                } else if (stretchingArret != null) {

                    val arret = stretchingArret!!
                    val midX = (arret.debut.x + arret.fin.x) / 2
                    val midY = (arret.debut.y + arret.fin.y) / 2
                    val dx = arret.fin.x - arret.debut.x
                    val dy = arret.fin.y - arret.debut.y
                    val len = max(1f, sqrt(dx*dx + dy*dy))
                    val nx = -dy / len
                    val ny = dx / len
                    

                    val vux = x - midX
                    val vuy = y - midY
                    val courbure = (vux * nx + vuy * ny) / 2f
                    onCourbureChanged(arret, courbure)
                    invalidate()
                }
                if (connectionStartNode != null) {
                    currentTouchPoint.set(x, y)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (connectionStartNode != null) {
                    val endNode = findNodeAt(x, y)
                    if (endNode != null && endNode != connectionStartNode) {
                        onConnectionCreated(connectionStartNode!!, endNode)
                    }
                    connectionStartNode = null
                    invalidate()
                }
                draggingNode = null
                stretchingArret = null
            }
        }
        return true
    }

    private fun findNodeAt(x: Float, y: Float): Noeud? {
        return graph.noeuds.find { sqrt((it.x - x).pow(2) + (it.y - y).pow(2)) < 70f }
    }

    private fun findArretAt(x: Float, y: Float): Arret? {
        return graph.arrets.find { arret ->
            val path = getArretPath(arret)
            val pm = PathMeasure(path, false)
            val pos = FloatArray(2)
            pm.getPosTan(pm.length * 0.5f, pos, null)
            // Vérifier si on touche près du milieu/label
            sqrt((pos[0] - x).pow(2) + (pos[1] - y).pow(2)) < 80f
        }
    }
}