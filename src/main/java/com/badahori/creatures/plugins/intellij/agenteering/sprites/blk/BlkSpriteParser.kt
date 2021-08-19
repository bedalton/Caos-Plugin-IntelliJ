package com.badahori.creatures.plugins.intellij.agenteering.sprites.blk

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.awt.image.BufferedImage
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.Future


/**
 * Parses Creatures BLK sprite file
 */
class BlkSpriteFile(private val file: VirtualFile) : SpriteFile<BlkSpriteFrame>(SpriteType.BLK) {

    private var cellsWide: Int = 0
    private var cellsHigh: Int = 0
    private lateinit var mStitched: Future<Pair<BufferedImage, Boolean>>
    private val md5: String
    lateinit var encoding:ColorEncoding
    private lateinit var mImages:List<BufferedImage?>

    override val images:List<BufferedImage?> get() {
        if (this::mImages.isInitialized)
            return this.mImages
        mImages = getAndCacheImages()
        return mImages
    }

    init {
        // Init buffer
        val bytes = file.getAllBytes()
        val md = MessageDigest.getInstance("MD5")
        md5 = BigInteger(1, md.digest(bytes)).toString(16).padStart(32, '0')
        val oldMD5 = file.getUserData(HASH_KEY)
        var didLoadCache = false
        if (oldMD5 == md5) {
            val cache = file.getUserData(CACHE_KEY)
            if (cache != null) {
                didLoadCache = true
                encoding = if (cache.is565) ColorEncoding.X_565 else ColorEncoding.X_555
                mFrames = cache.images.map { image ->
                    BlkSpriteFrame(image, encoding)
                }
                this.cellsWide = cache.cellsWide
                this.cellsHigh = cache.cellsHigh
            }
        }
        if (!didLoadCache) {
            readDelayed(bytes)
        }
    }

    private fun readDelayed(bytes: ByteArray) {
        val totalBytes = bytes.size
        val bytesBuffer = ByteBuffer.wrap(bytes).littleEndian()
        val flags = bytesBuffer.uInt32
        encoding = if (flags and 1L == 1L) ColorEncoding.X_565 else ColorEncoding.X_555

        // Get number of cells wide/high
        val cellsWide = bytesBuffer.uInt16
        val cellsHigh = bytesBuffer.uInt16

        // Set cells width/height for instance
        this.cellsWide = cellsWide
        this.cellsHigh = cellsHigh

        // Read total cells and validate
        val totalCells = bytesBuffer.uInt16
        if (cellsWide * cellsHigh != totalCells) {
            LOGGER.severe("Sprite cells width x height do not match total cells")
        }
        mFrames = (0 until totalCells).map { i ->
            val offsetForData = bytesBuffer.uInt32 + 4
            if (offsetForData + CELL_BYTES_SIZE > totalBytes) {
                throw Exception("Invalid byte offset[$i]. Expected < ${totalBytes - CELL_BYTES_SIZE}. Found: $offsetForData")
            }
            val width = bytesBuffer.uInt16
            if (width != BLOCK_SIZE) {
                throw Exception("BLK cell width invalid. Expected 128; Actual: $width")
            }
            val height = bytesBuffer.uInt16
            if (height != BLOCK_SIZE) {
                throw Exception("BLK cell height invalid. Expected 128; Actual: $height")
            }

            BlkSpriteFrame(
                bytes = bytesBuffer,
                offset = offsetForData,
                width = width,
                height = height,
                encoding = encoding
            )
        }
    }

    override fun compile(): ByteArray {
        TODO("Not yet implemented")
    }

    fun getStitched(): Future<Pair<BufferedImage, Boolean>> {
        // Returned stitch if already exists
        if (this::mStitched.isInitialized)
            return this.mStitched

        return ApplicationManager.getApplication().executeOnPooledThread(Callable {
            // Calculate image size
            val trueWidth = cellsWide * BLOCK_SIZE
            val trueHeight = cellsHigh * BLOCK_SIZE

            // Create image for writing
            val temp = BufferedImage(trueWidth, trueHeight, BufferedImage.TYPE_INT_RGB)
            val graphics2d = temp.createGraphics()
            var didProcessAll = true
            repeat(cellsWide) repeatX@{ cellX ->
                val x = cellX * BLOCK_SIZE
                repeat(cellsHigh) { cellY ->
                    val y = cellY * BLOCK_SIZE
                    // Ensure that cell was decoded
                    val cell: BufferedImage? = images[(cellX * cellsHigh) + cellY]
                    if (cell == null) {
                        didProcessAll = false
                        return@repeatX
                    }
                    graphics2d.drawImage(cell, x, y, null)
                }
            }
            Pair(temp, didProcessAll)
        })

    }

    private fun getAndCacheImages() : List<BufferedImage?> {
        return super.images.apply {
            file.putUserData(
                CACHE_KEY, Cache(
                    cellsWide,
                    cellsHigh,
                    this,
                    encoding == ColorEncoding.X_565
                )
            )
            file.putUserData(HASH_KEY, md5)
        }
    }

    companion object {
        const val BLOCK_SIZE = 128
        const val CELL_BYTES_SIZE = BLOCK_SIZE * BLOCK_SIZE * 2
        private val HASH_KEY = Key<String>("creatures.sprites.blk.CACHE_FILE_HASH")
        private val CACHE_KEY = Key<Cache>("creatures.sprites.blk.CACHE")
    }
}

class BlkSpriteFrame private constructor(width: Int, height: Int, private val encoding: ColorEncoding) :
    SpriteFrame<BlkSpriteFrame>(width, height, SpriteType.BLK) {

    private lateinit var getImage: () -> BufferedImage?

    constructor(bytes: ByteBuffer, offset: Long, width: Int, height: Int, encoding: ColorEncoding) : this(
        width,
        height,
        encoding
    ) {
        getImage = {
            decode(bytes, offset)
        }
    }

    constructor(image: BufferedImage?, encoding: ColorEncoding = ColorEncoding.X_565) : this(
        image?.width ?: 0,
        image?.height ?: 0,
        encoding
    ) {
        getImage = { image }
    }

    override fun decode(): BufferedImage? {
        if (this::getImage.isInitialized)
            return getImage()
        return null
    }

    private fun decode(bytes: ByteBuffer, offset: Long): BufferedImage {
        val bytesBuffer = bytes.duplicate()
        bytesBuffer.position(offset.toInt())
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val toRgb = encoding.toRgb
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bytesBuffer.uInt16
                val color = toRgb(pixel)
                image.raster.setPixel(x, y, color)
                image.alphaRaster.setPixel(x, y, SpriteColorUtil.solid)
            }
        }
        return image
    }

    override fun encode(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun copy(): BlkSpriteFrame {
        val myGetImage = this.getImage
        return BlkSpriteFrame(width, height, encoding).apply {
            getImage = myGetImage
        }
    }

    fun copy(encoding: ColorEncoding): BlkSpriteFrame {
        val myGetImage = this.getImage
        return BlkSpriteFrame(width, height, encoding).apply {
            getImage = myGetImage
        }
    }
}

private data class Cache(val cellsWide: Int, val cellsHigh: Int, val images: List<BufferedImage?>, val is565: Boolean)
