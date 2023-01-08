package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.bedalton.io.bytes.*
import bedalton.creatures.sprite.parsers.PhotoAlbum
import bedalton.creatures.sprite.parsers.SpriteParser.parsePhotoAlbum
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteFileHolder
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser.parse
import com.bedalton.log.Log
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import javax.swing.JLabel
import kotlin.math.ceil

object SpriteEditorViewParser {
    @JvmStatic
    fun parse(file: VirtualFile, stream: ByteStreamReader, loadingLabel: () -> JLabel?, initPlaceholder: () -> JLabel): PhotoAlbum? {
        return runBlocking {
            parsePhotoAlbum(file.nameWithoutExtension, stream, 1) { i, total ->
                val progress = ceil(i * 100.0 / total)
                ApplicationManager.getApplication().invokeLater {
                    if (file.isValid) {
                        loadingLabel()?.text = "Loading sprite... " + progress.toInt() + "%"
                    } else {
                        initPlaceholder().text = "Sprite file invalidated"
                    }
                }
                true
            }
        }
    }

    @JvmStatic
    fun parseSprite(file: VirtualFile, loadingLabel: () -> JLabel): SpriteFileHolder {
        if (!file.isValid) {
            throw Exception("Cannot parse sprite file with invalid virtual file")
        }
        return runBlocking {
            parse(file) { i, total ->
                val progress = ceil(i * 100.0 / total)
                val label = loadingLabel()
                ApplicationManager.getApplication().invokeLater {
                    if (file.isValid) {
                        label.text = "Loading sprite... " + progress.toInt() + "%"
                    } else {
                        label.text = "Sprite file invalidated"
                    }
                }
                true
            }
        }
    }
}