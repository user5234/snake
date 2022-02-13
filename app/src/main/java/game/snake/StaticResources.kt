package game.snake

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint


object BitMaps {
    fun get(@DrawableRes res : Int) : Bitmap {
        return BitmapFactory.decodeResource(MainActivity.instance.resources, res)
    }
}

object Drawables {
    fun get(@DrawableRes res : Int) : Drawable? {
        return ResourcesCompat.getDrawable(MainActivity.instance.resources, res, null)
    }
}

object Background {
    /**
     * @return a wonderful bit of magic
     */
    fun get(width : Int, height : Int, unitSize : Int) : Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        for (i in 0..height - 2 * unitSize step unitSize) {
            for (j in unitSize..width - 2 * unitSize step unitSize) {
                if (((i + j) / unitSize) % 2 == 0) {
                    paint.color = Color.rgb(245, 164, 98)
                }
                else {
                    paint.color = Color.rgb(239, 87, 86)
                }
                canvas.drawRect(j.toFloat(), i.toFloat(), j.toFloat() + unitSize, i.toFloat() + unitSize, paint)
            }
        }
        paint.color = Color.rgb(76, 8, 117)
        canvas.drawRect(unitSize * 3 / 4F, 0F, unitSize.toFloat(), height.toFloat() - unitSize - height % unitSize, paint) //left
        canvas.drawRect(width.toFloat() - unitSize - width % unitSize, 0F, width - 3 * unitSize / 4F - width % unitSize, height.toFloat() - unitSize - height % unitSize, paint) //right
        canvas.drawRect(unitSize * 3 / 4F, height.toFloat() - unitSize - height % unitSize, width - unitSize * 3 / 4F - width % unitSize, height - height % unitSize - unitSize * 3 / 4F , paint) //bottom
        return bitmap
    }
}