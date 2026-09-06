package com.tanmay.focuslauncher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class LauncherView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val apps = mutableListOf<ApplicationInfo>()
    private val shownApps = mutableListOf<ApplicationInfo>()
    private var drawerOpen = false
    private var downY = 0f
    private var downX = 0f

    private val bg = Color.rgb(6, 6, 7)
    private val primary = Color.rgb(245, 245, 245)
    private val secondary = Color.rgb(125, 125, 130)
    private val card = Color.rgb(18, 18, 20)
    private val stroke = Color.rgb(38, 38, 41)

    init {
        isFocusable = true
        loadApps()
    }

    private fun loadApps() {
        apps.clear()
        val pm = context.packageManager
        apps.addAll(
            pm.getInstalledApplications(0)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .filter { it.packageName != context.packageName }
                .sortedBy { pm.getApplicationLabel(it).toString().lowercase(Locale.getDefault()) }
        )
        shownApps.clear()
        shownApps.addAll(apps.take(12))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bg)
        if (drawerOpen) drawDrawer(canvas) else drawHome(canvas)
    }

    private fun drawHome(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f

        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER
        paint.color = primary
        paint.textSize = 64f
        canvas.drawText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), cx, h * .22f, paint)

        paint.color = secondary
        paint.textSize = 14f
        canvas.drawText(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), cx, h * .22f + 30f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.color = primary
        paint.textSize = 13f
        canvas.drawText("FAVOURITES", 24f, h * .46f, paint)

        val gap = 10f
        val side = 24f
        val itemW = (w - side * 2 - gap * 3) / 4f
        val top = h * .49f
        for (i in 0 until minOf(4, shownApps.size)) {
            drawApp(canvas, shownApps[i], side + i * (itemW + gap), top, itemW, itemW)
        }

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.color = secondary
        paint.textSize = 12f
        canvas.drawText("Swipe up for apps", cx, h - 38f, paint)
    }

    private fun drawApp(canvas: Canvas, info: ApplicationInfo, x: Float, y: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.color = card
        canvas.drawRoundRect(x, y, x + w, y + h, 20f, 20f, paint)

        val icon: Drawable = info.loadIcon(context.packageManager)
        val inset = 15f
        icon.setBounds((x + inset).toInt(), (y + inset).toInt(), (x + w - inset).toInt(), (y + h - inset).toInt())
        icon.draw(canvas)

        paint.textAlign = Paint.Align.CENTER
        paint.color = secondary
        paint.textSize = 10f
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val label = context.packageManager.getApplicationLabel(info).toString()
        canvas.drawText(label.take(12), x + w / 2f, y + h + 18f, paint)
    }

    private fun drawDrawer(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.color = primary
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textSize = 26f
        canvas.drawText("All apps", 24f, 58f, paint)

        paint.color = secondary
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 12f
        canvas.drawText("Swipe down to close", 24f, 80f, paint)

        val columns = 4
        val gap = 10f
        val side = 24f
        val itemW = (w - side * 2 - gap * (columns - 1)) / columns
        val itemH = itemW
        val top = 105f
        val rows = minOf(5, (apps.size + columns - 1) / columns)

        for (i in 0 until minOf(apps.size, rows * columns)) {
            val col = i % columns
            val row = i / columns
            drawApp(canvas, apps[i], side + col * (itemW + gap), top + row * (itemH + 38f), itemW, itemH)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dy = event.y - downY
                if (!drawerOpen && dy < -100f) {
                    drawerOpen = true
                    invalidate()
                    return true
                }
                if (drawerOpen && dy > 100f) {
                    drawerOpen = false
                    invalidate()
                    return true
                }
                if (!drawerOpen && downY > height * .48f && downY < height * .70f) {
                    val index = ((downX - 24f) / ((width - 48f - 30f) / 4f + 10f)).toInt()
                    if (index in 0 until minOf(4, shownApps.size)) {
                        (context as? MainActivity)?.openApp(shownApps[index])
                    }
                } else if (drawerOpen && downY > 100f) {
                    val columns = 4
                    val gap = 10f
                    val side = 24f
                    val itemW = (width - side * 2 - gap * 3) / 4f
                    val col = ((downX - side) / (itemW + gap)).toInt()
                    val row = ((downY - 105f) / (itemW + 38f)).toInt()
                    val index = row * columns + col
                    if (col in 0 until columns && row >= 0 && index in apps.indices) {
                        (context as? MainActivity)?.openApp(apps[index])
                    }
                }
                return true
            }
        }
        return true
    }
}
