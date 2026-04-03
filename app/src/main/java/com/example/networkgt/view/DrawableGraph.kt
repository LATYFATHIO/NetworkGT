package com.example.networkgt.view

import android.graphics.*
import android.graphics.drawable.Drawable
import com.example.networkgt.model.Arret
import com.example.networkgt.model.Graph
import com.example.networkgt.model.Noeud
import kotlin.math.*

class DrawableGraph(
    private val graph: Graph,
    private val backgroundPlan: Bitmap? = null
) : Drawable() {

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

    override fun draw(canvas: Canvas) {
        // 1. Draw Background Plan
        backgroundPlan?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        // 2. Draw Edges
        graph.arrets.forEach { drawArret(canvas, it) }

        // 3. Draw Nodes
        graph.noeuds.forEach { node ->
            paintNode.color = node.color
            val rect = RectF(node.x - 50f, node.y - 50f, node.x + 50f, node.y + 50f)
            canvas.drawRoundRect(rect, 20f, 20f, paintNode)
            canvas.drawText(node.label, node.x, node.y - 65f, paintText)
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

    override fun setAlpha(alpha: Int) {
        paintNode.alpha = alpha
        paintEdge.alpha = alpha
        paintText.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paintNode.colorFilter = colorFilter
        paintEdge.colorFilter = colorFilter
        paintText.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    /**
     * Helper to create a Bitmap from this Drawable
     */
    fun toBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, width, height)
        draw(canvas)
        return bitmap
    }
}