package com.badahori.creatures.plugins.intellij.agenteering.utils

import org.apache.commons.imaging.palette.Dithering
import org.apache.commons.imaging.palette.Palette
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.xml.bind.DatatypeConverter

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

fun BufferedImage.createTransformed(at: AffineTransform) : BufferedImage {
    val newImage = BufferedImage(
            width, height,
            BufferedImage.TYPE_INT_ARGB)
    val g = newImage.createGraphics()
    g.transform(at)
    g.drawImage(this, 0, 0, null)
    g.dispose()
    return newImage
}

fun BufferedImage.flipVertical() : BufferedImage {
    val at = AffineTransform()
    at.concatenate(AffineTransform.getScaleInstance(1.0, -1.0))
    at.concatenate(AffineTransform.getTranslateInstance(0.0, -height.toDouble()))
    return createTransformed(at)
}
fun BufferedImage.flipHorizontal() : BufferedImage {
    val at = AffineTransform()
    at.concatenate(AffineTransform.getScaleInstance(-1.0, 1.0))
    at.concatenate(AffineTransform.getTranslateInstance(-width.toDouble(), 0.0))
    return createTransformed(at)
}

fun RenderedImage.toPngByteArray() : ByteArray {
    return toByteArray("png")
}

fun RenderedImage.toJpgByteArray() : ByteArray {
    return toByteArray("jpg")
}

fun RenderedImage.toByteArray(formatName:String) : ByteArray {
    ImageIO.getWriterFormatNames().let { formats ->
        assert(formatName in formats) {
            "Cannot convert image to byte array for format: '$formatName'. Available types: [${formats.joinToString()}]"
        }
    }
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(this, formatName, outputStream)
    return outputStream.toByteArray()
}

fun RenderedImage.toPngDataUri() : String {
    return "data:image/png;base64," + DatatypeConverter.printBase64Binary(toPngByteArray())
}
fun RenderedImage.toJpgDataUri() : String {
    return "data:image/jpg;base64," + DatatypeConverter.printBase64Binary(toJpgByteArray())
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