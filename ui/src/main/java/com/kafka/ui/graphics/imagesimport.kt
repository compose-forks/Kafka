/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kafka.ui.graphics

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Draw
import androidx.ui.geometry.Rect
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.FilterQuality
import androidx.ui.graphics.Image
import androidx.ui.graphics.ImageConfig
import androidx.ui.graphics.NativeImage
import androidx.ui.graphics.Paint
import androidx.ui.graphics.colorspace.ColorSpace
import androidx.ui.graphics.colorspace.ColorSpaces

/**
 * An import of `DrawImage` from `Image.kt` in `ui-foundation`. This function contains a small
 * change to allow modification of the [Paint] instance via a lambda.
 */
@Composable
fun DrawImage(
    image: Image,
    tint: Color? = null,
    paintModifier: @Composable ((Paint) -> Unit)? = null
) {
    val paint = remember {
        Paint().apply {
            filterQuality = FilterQuality.low // we only support low currently
        }
    }
    paint.colorFilter = tint?.let { ColorFilter(it, BlendMode.srcIn) }
    paintModifier?.invoke(paint)

    Draw { canvas, parentSize ->
        val inputWidth = image.width.toFloat()
        val inputHeight = image.height.toFloat()
        val inputAspectRatio = inputWidth / inputHeight

        val outputWidth = parentSize.width.value
        val outputHeight = parentSize.height.value
        val outputAspectRatio = outputWidth / outputHeight

        val fittedWidth = if (outputAspectRatio > inputAspectRatio) {
            inputWidth
        } else {
            inputHeight * outputAspectRatio
        }
        val fittedHeight = if (outputAspectRatio > inputAspectRatio) {
            inputWidth / outputAspectRatio
        } else {
            inputHeight
        }

        val srcRect = Rect(
            left = (inputWidth - fittedWidth) / 2,
            top = (inputHeight - fittedHeight) / 2,
            right = (inputWidth + fittedWidth) / 2,
            bottom = (inputHeight + fittedHeight) / 2
        )

        val dstRect = Rect(
            left = 0f,
            top = 0f,
            right = outputWidth,
            bottom = outputHeight
        )
        canvas.drawImageRect(image, srcRect, dstRect, paint)
    }
}

/**
 * This is a copy of the internal `AndroidImage` class from the `ui-foundation` source
 */
class AndroidImage(val bitmap: Bitmap) : Image {

    /**
     * @see Image.width
     */
    override val width: Int
        get() = bitmap.width

    /**
     * @see Image.height
     */
    override val height: Int
        get() = bitmap.height

    override val config: ImageConfig
        get() = bitmap.config.toImageConfig()

    /**
     * @see Image.colorSpace
     */
    override val colorSpace: ColorSpace
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bitmap.colorSpace?.toComposeColorSpace() ?: ColorSpaces.Srgb
        } else {
            ColorSpaces.Srgb
        }

    /**
     * @see Image.hasAlpha
     */
    override val hasAlpha: Boolean
        get() = bitmap.hasAlpha()

    /**
     * @see Image.nativeImage
     */
    override val nativeImage: NativeImage
        get() = bitmap

    /**
     * @see
     */
    override fun prepareToDraw() {
        bitmap.prepareToDraw()
    }
}

private fun Bitmap.Config.toImageConfig(): ImageConfig {
    // Cannot utilize when statements with enums that may have different sets of supported
    // values between the compiled SDK and the platform version of the device.
    // As a workaround use if/else statements
    // See https://youtrack.jetbrains.com/issue/KT-30473 for details
    @Suppress("DEPRECATION")
    return if (this == Bitmap.Config.ALPHA_8) {
        ImageConfig.Alpha8
    } else if (this == Bitmap.Config.RGB_565) {
        ImageConfig.Rgb565
    } else if (this == Bitmap.Config.ARGB_4444) {
        ImageConfig.Argb8888 // Always upgrade to Argb_8888
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == Bitmap.Config.RGBA_F16) {
        ImageConfig.F16
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == Bitmap.Config.HARDWARE) {
        ImageConfig.Gpu
    } else {
        ImageConfig.Argb8888
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun android.graphics.ColorSpace.toComposeColorSpace() = when (this) {
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB) -> ColorSpaces.Srgb
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ACES) -> ColorSpaces.Aces
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ACESCG) -> ColorSpaces.Acescg
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ADOBE_RGB) -> ColorSpaces.AdobeRgb
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.BT2020) -> ColorSpaces.Bt2020
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.BT709) -> ColorSpaces.Bt709
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.CIE_LAB) -> ColorSpaces.CieLab
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.CIE_XYZ) -> ColorSpaces.CieXyz
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.DCI_P3) -> ColorSpaces.DciP3
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.DISPLAY_P3) -> ColorSpaces.DisplayP3
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.EXTENDED_SRGB) -> ColorSpaces.ExtendedSrgb
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_EXTENDED_SRGB) -> ColorSpaces.LinearExtendedSrgb
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_SRGB) -> ColorSpaces.LinearSrgb
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.NTSC_1953) -> ColorSpaces.Ntsc1953
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.PRO_PHOTO_RGB) -> ColorSpaces.ProPhotoRgb
    android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SMPTE_C) -> ColorSpaces.SmpteC
    else -> ColorSpaces.Srgb
}
