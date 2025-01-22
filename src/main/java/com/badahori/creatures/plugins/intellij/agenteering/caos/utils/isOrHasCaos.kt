package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.bedalton.common.structs.Pointer
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import kotlinx.coroutines.runBlocking



internal val IS_OR_HAS_CAOS_FILES_DATA_KEY = DataKey.create<Boolean>("creatures.is_or_has_caos_file")

internal fun isOrHasCreaturesFiles(files: Array<VirtualFile>): Boolean {
    for (file in files) {
        if (isOrHasCaosFile(file)) {
            return true
        }
    }
    return false
}

internal fun isOrHasCreaturesFiles(files: Iterable<VirtualFile>): Boolean {
    for (file in files) {
        if (isOrHasCaosFile(file)) {
            return true
        }
    }
    return false
}

internal fun isOrHasCaosFile(file: VirtualFile): Boolean = runReadAction {
    runBlocking {
        isOrHasCaosFile(file, Pointer(0))
    }
}

private fun isOrHasCaosFile(file: VirtualFile, count: Pointer<Int>): Boolean {
    var isCaos = false
    VfsUtilCore.visitChildrenRecursively(file, object : VirtualFileVisitor<Boolean>() {

        override fun visitFile(file: VirtualFile): Boolean {
            if (count.value > MAX_FILES_CHECKED) {
                isCaos = true
                return false
            }

            if (isCaos) {
                return false
            }

            if (file.isDirectory) {
                return true
            }

            if (file.fileType == CaosScriptFileType.INSTANCE || file.extension?.lowercase() in expectedExtensions) {
                isCaos = true
            }

            return !isCaos
        }

        override fun visitFileEx(file: VirtualFile): Result {
            if (file.isDirectory && file.name == ".idea") {
                return SKIP_CHILDREN
            }
            return if (isCaos || count.value > MAX_FILES_CHECKED) {
                SKIP_CHILDREN
            } else {
                return super.visitFileEx(file)
            }
        }
    })
    return isCaos
}
//
//private fun isOrHasCaosFile(file: VirtualFile, level: Int, count: Pointer<Int>): Boolean {
//    count.value += 1
//    if (count.value > MAX_FILES_CHECKED) {
//        return true
//    }
//
//
//    // Ensure not searching too deep
//    val childLevel = level + 1
//    if (childLevel > MAX_LEVELS) {
//        return false
//    }
//
//    // Do not search children for as long
//    val childStartCount = childLevel * LEVEL_CHECK_MOD
//    val thisCounter = Pointer(childStartCount)
//
//    for (child in file.children.orEmpty()) {
//        if (isOrHasCaosFile(file, level = childLevel, thisCounter)) {
//            return true
//        }
//        if (thisCounter.value > MAX_FILES_CHECKED) {
//            break
//        }
//    }
//    return false
//}

private const val MAX_FILES_CHECKED = 80

private val expectedExtensions = listOf(
    "ps",
    "cos",
    "att",
    "spr",
    "s16",
    "c16",
    "blk"
)
