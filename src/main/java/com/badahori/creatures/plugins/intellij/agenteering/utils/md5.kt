@file:OptIn(ExperimentalStdlibApi::class)

package com.badahori.creatures.plugins.intellij.agenteering.utils

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest


fun File.md5() : String? {
    return MD5.fromFile(this)
}

object MD5 {

    @JvmStatic
    fun fromFile(file: File): String? {
        if (!file.exists()) {
            LOGGER.severe("Failed to get MD5. File '${file.path}' does not exist")
            return null
        }
        val md5Digest =  getMessageDigest()
            ?: return null
        //Get the checksum
        return try {
            getFileChecksum(md5Digest, file)
        } catch (e:Exception) {
            e.rethrowAnyCancellationException()
            LOGGER.severe("Failed to get checksum for file: ${file.path}. Error: " + e.localizedMessage)
            null
        }
    }

    @JvmStatic
    fun fromString(text: String) : String? {
       return fromBytes(text.encodeToByteArray())
    }

    @JvmStatic
    @Suppress("MemberVisibilityCanBePrivate")
    fun fromBytes(byteArray: ByteArray): String? {
        val md5Digest =  getMessageDigest()
            ?: return null
        //Get the checksum
        return try {
            getFileChecksum(md5Digest, byteArray)
        } catch (e:Exception) {
            e.rethrowAnyCancellationException()
            LOGGER.severe("Failed to get checksum for file. Error: " + e.localizedMessage)
            null
        }
    }

    private fun getMessageDigest(): MessageDigest? {
        return try {
            //Use MD5 algorithm
            //Use MD5 algorithm
            MessageDigest.getInstance("MD5")
        } catch (e:Exception) {
            e.rethrowAnyCancellationException()
            LOGGER.severe("Failed to create MD5 digest object. Error: " + e.localizedMessage)
            return null
        }
    }
}


private fun getFileChecksum(digest: MessageDigest, file: File): String {
    //Get file input stream for reading the file content
    val fis = FileInputStream(file)

    //Create byte array to read data in chunks
    val byteArray = ByteArray(1024)
    var bytesCount: Int

    //Read file data and update in message digest
    while (fis.read(byteArray).also { bytesCount = it } != -1) {
        digest.update(byteArray, 0, bytesCount)
    }

    //close the stream; We don't need it now.
    fis.close()

    //Get the hash's bytes
    val bytes: ByteArray = digest.digest()

    //These bytes[] has bytes in decimal format;
    //Convert it to hexadecimal format
    val sb = StringBuilder()
    for (i in bytes.indices) {
        sb.append(((bytes[i].toInt() and 0xff) + 0x100).toString(16).substring(1))
    }

    //return complete hash
    return sb.toString()
}


private fun getFileChecksum(digest: MessageDigest, byteArray: ByteArray): String {

    digest.update(byteArray, 0, byteArray.size)

    //Get the hash's bytes
    val bytes: ByteArray = digest.digest()

    // These bytes[] has bytes in decimal format;
    //Convert it to hexadecimal format
    val sb = StringBuilder()
    for (i in bytes.indices) {
        sb.append(((bytes[i].toInt() and 0xff) + 0x100).toString(16).substring(1))
    }

    //return complete hash
    return sb.toString()
}