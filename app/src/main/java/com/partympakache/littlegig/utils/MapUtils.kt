package com.partympakache.littlegig.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object MapUtils {

    fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Corrected extension function to convert BitmapDescriptor to Bitmap
    fun BitmapDescriptor.toBitmap(context: Context): Bitmap {
        // Handle the case where the BitmapDescriptor *wasn't* created from a vector
        val defaultBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Placeholder
        val drawable =  getIconDrawable(context) // Use a helper function

        if (drawable != null) { //Check
            if (drawable is BitmapDrawable) {
                return drawable.bitmap // If it's already a BitmapDrawable, just return the Bitmap
            }

            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.takeIf { it > 0 } ?: 1, // Ensure width/height > 0
                drawable.intrinsicHeight.takeIf { it > 0 } ?: 1,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        } else {
            // Handle cases
            return defaultBitmap // Return a default.

        }
    }
    //Gets a drawable object.
    private fun BitmapDescriptor.getIconDrawable(context: Context): Drawable? {
        // This part requires reflection, which *can* have performance implications
        // and might break in future versions of the Maps SDK.  It's necessary
        // because the internal structure of BitmapDescriptor is not public.
        return try {
            val field = this.javaClass.getDeclaredField("zza") // Accessing the internal field
            field.isAccessible = true // Making it accessible (even if private)
            val remoteObject = field.get(this) // Get the value of the field (the RemoteObject)

            val method = remoteObject.javaClass.getMethod("zza") // Get the method we need
            method.isAccessible = true
            val resourceId = method.invoke(remoteObject) as Int // Invoke, get the resource ID

            ContextCompat.getDrawable(context, resourceId) // Get the drawable

        } catch (e: Exception) {
            // Handle reflection exceptions.  In a production app, log this
            // and potentially provide a fallback mechanism (e.g., a default icon).
            null  // Return null
        }
    }

}