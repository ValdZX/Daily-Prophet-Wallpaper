package ua.valdzx.wallpaper.dailyprophet

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.AnyRes
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.IOException


class LiveWallpaperService : WallpaperService() {

    inner class LiveEngine : Engine() {
        var surfholder: SurfaceHolder? = null
        val callbackHandler = Handler(this@LiveWallpaperService.mainLooper)
        var isVisiable = true
        var back: Bitmap? = null
        private val region: Rect = Rect()
        private val paint: Paint = Paint()
        private var xOffset = 0.0f
        private var xOffsetStep = 0.0f
        private var retriever: MediaMetadataRetriever? = null
        private var duration: Int = 0


        val drawloopfun = Runnable { draw() }

        private fun draw() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                surfholder?.lockHardwareCanvas()
            } else {
                surfholder?.lockCanvas()
            }?.let { canvas ->
                back?.let { bitmap ->
                    val bHeight = bitmap.height
                    val bWidth = bitmap.width
                    val cWidth = canvas.width
                    val cHeight = canvas.height
                    val width = (bWidth * cHeight) / bHeight
                    val offset = (cWidth - width) * xOffset
                    region.set(0, 0, width, cHeight)
                    region.offset(offset.toInt(), 0)
                    canvas.drawBitmap(bitmap, null, region, paint)
                    val time = (System.currentTimeMillis() % duration) * 1000
//                    retriever
//                        ?.getFrameAtTime(time)
//                        ?.let { videoBitmap ->
//                            canvas.drawBitmap(videoBitmap, 0f,0f, null)
//                        }
                }
                surfholder?.unlockCanvasAndPost(canvas)
            }
            callbackHandler.removeCallbacks(drawloopfun)
            if (isVisiable) {
                callbackHandler.postDelayed(drawloopfun, 40)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            paint.isFilterBitmap = true
            back = BitmapFactory.decodeResource(resources, R.drawable.back)
            val firstVideoUri = getResourceUri(R.raw.resized)
            retriever = MediaMetadataRetriever()
            retriever?.setDataSource(this@LiveWallpaperService, firstVideoUri)
            val mp = MediaPlayer.create(this@LiveWallpaperService, firstVideoUri)
            duration = mp.duration


        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            surfholder = surfaceHolder
            callbackHandler.post(drawloopfun)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisiable = visible
            if (visible) {
                callbackHandler.post(drawloopfun)
            } else {
                callbackHandler.removeCallbacks(drawloopfun)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            callbackHandler.removeCallbacks(drawloopfun)
            surfholder = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            this.xOffset = xOffset
            this.xOffsetStep = xOffsetStep
        }
    }

    override fun onCreateEngine(): Engine {
        return LiveEngine()
    }

    companion object {
        fun setToWallPaper(context: Context) {
            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, LiveWallpaperService::class.java)
                )
            }.also {
                context.startActivity(it)
            }
            try {
                WallpaperManager.getInstance(context).clear()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        internal fun Context.getResourceUri(@AnyRes resourceId: Int): Uri =
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(packageName)
                .path(resourceId.toString())
                .build()
    }
}

