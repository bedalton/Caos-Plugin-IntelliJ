package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.bedalton.io.bytes.toBase64
import com.bedalton.log.Log
import org.apache.commons.imaging.palette.Dithering
import org.apache.commons.imaging.palette.Palette
import java.awt.*
import java.awt.datatransfer.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.ImageObserver
import java.awt.image.RenderedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun BufferedImage.copy(): BufferedImage {
    val b = BufferedImage(width, height, type);
    val g: Graphics2D = b.createGraphics();
    g.drawImage(this, 0, 0, null);
    g.dispose();
    return b;
}

fun BufferedImage.ditherInPlace(palette: Palette) {
    Dithering.applyFloydSteinbergDithering(this, palette)
}

fun BufferedImage.ditherCopy(palette: Palette): BufferedImage {
    return copy().apply {
        Dithering.applyFloydSteinbergDithering(this, palette)
    }
}

fun BufferedImage.createTransformed(at: AffineTransform): BufferedImage {
    val newImage = BufferedImage(
        width, height,
        BufferedImage.TYPE_INT_ARGB
    )
    val g = newImage.createGraphics()
    g.transform(at)
    g.drawImage(this, 0, 0, null)
    g.dispose()
    return newImage
}

fun BufferedImage.flipVertical(): BufferedImage {
    val at = AffineTransform()
    at.concatenate(AffineTransform.getScaleInstance(1.0, -1.0))
    at.concatenate(AffineTransform.getTranslateInstance(0.0, -height.toDouble()))
    return createTransformed(at)
}

fun BufferedImage.flipHorizontal(): BufferedImage {
    val at = AffineTransform()
    at.concatenate(AffineTransform.getScaleInstance(-1.0, 1.0))
    at.concatenate(AffineTransform.getTranslateInstance(-width.toDouble(), 0.0))
    return createTransformed(at)
}

fun RenderedImage.toPngByteArray(): ByteArray {
    return toByteArray("png")
}

fun RenderedImage.toJpgByteArray(): ByteArray {
    return toByteArray("jpg")
}

fun RenderedImage.toByteArray(formatName: String): ByteArray {
    ImageIO.getWriterFormatNames().let { formats ->
        assert(formatName in formats) {
            "Cannot convert image to byte array for format: '$formatName'. Available types: [${formats.joinToString()}]"
        }
    }
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(this, formatName, outputStream)
    return outputStream.toByteArray()
}

fun RenderedImage.toPngDataUri(): String {
    return "data:image/png;base64," + toPngByteArray().toBase64()
}

fun RenderedImage.toJpgDataUri(): String {
    return "data:image/jpg;base64," + toJpgByteArray().toBase64()
}


fun BufferedImage.copyToClipboard() {
    val clipboardOwner = CopyImageToClipBoard()
    val trans: CopyImageToClipBoard.TransferableImage = CopyImageToClipBoard.TransferableImage(this)
    val c: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    c.setContents(trans, clipboardOwner)
}

private class CopyImageToClipBoard : ClipboardOwner {

    override fun lostOwnership(clip: Clipboard?, trans: Transferable?) {
    }

    class TransferableImage(val image: Image?) : Transferable {
        override fun getTransferData(flavor: DataFlavor): Any {
            return if (flavor.equals(DataFlavor.imageFlavor) && image != null) {
                image
            } else {
                throw UnsupportedFlavorException(flavor)
            }
        }

        override fun getTransferDataFlavors(): Array<DataFlavor?> {
            val flavors: Array<DataFlavor?> = arrayOfNulls(1)
            flavors[0] = DataFlavor.imageFlavor
            return flavors
        }

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
            val flavors: Array<DataFlavor?> = transferDataFlavors
            for (i in flavors.indices) {
                if (flavor.equals(flavors[i])) {
                    return true
                }
            }
            return false
        }
    }
}


/**
 * Compares two images pixel by pixel.
 * @param otherImage the second image.
 * @return whether the images are both the same or not.
 */
internal fun BufferedImage.contentsEqual(otherImage: BufferedImage): Boolean {
    // The images must be the same size.
    if (width != otherImage.width || height != otherImage.height) {
        return false;
    }

    val width = width
    val height = height

    // Loop over every pixel.
    repeat(height) { y ->
        repeat(width) { x ->
            // Compare the pixels for equality.
            if (getRGB(x, y) != otherImage.getRGB(x, y)) {
                return false;
            }
        }
    }

    return true;
}


private val mMediaTracker = MediaTracker(Panel())
private val mImageObserver = ImageObserver { _, _, _, _, _, _ -> true }

private fun waitForImage(image: Image) {
    try {
        mMediaTracker.addImage(image, 0)
        mMediaTracker.waitForID(0)
    } finally {
        mMediaTracker.removeImage(image)
    }
}

val Image.width: Int
    get() {
        waitForImage(this)
        return getWidth(mImageObserver)
    }

val Image.height: Int
    get() {
        waitForImage(this)
        return getHeight(mImageObserver)
    }

fun Image.scaleNearestNeighbor(scale: Double): BufferedImage {
    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()
    val destinationBufferedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
    val g2: Graphics2D = destinationBufferedImage.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
    g2.drawImage(this, 0, 0, newWidth, newHeight, null)
    g2.dispose()
    return destinationBufferedImage
}

fun BufferedImage.trim(): BufferedImage {
    var minX: Int? = null
    var maxX: Int? = null
    var minY: Int? = null
    var maxY: Int? = null

    repeat(height) { y ->
        repeat(width) x@{ x ->
            val pixel = getRGB(x, y)

            if ((pixel shr 24) == 0x00) {
                return@x
            }

            if (minY == null || y < minY!!) {
                minY = y
            }
            if (maxY == null || y > maxY!!) {
                maxY = y
            }
            if (minX == null || x < minX!!) {
                minX = x
            }
            if (maxX == null || x > maxX!!) {
                maxX = x
            }
        }
    }

    if (minX == null || maxX == null || minY == null || maxY == null) {
        Log.i("No solid pixels")
        return blankImage
    }

    val trimmedWidth = (maxX!! - minX!!) + 1
    val trimmedHeight = (maxY!! - minY!!) + 1
    Log.i("Trimming image ${width}x$height to ${trimmedWidth}x$trimmedHeight")
    return getSubimage(minX!!, minY!!, trimmedWidth, trimmedHeight)
}

private val blankImage by lazy {
    BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
}