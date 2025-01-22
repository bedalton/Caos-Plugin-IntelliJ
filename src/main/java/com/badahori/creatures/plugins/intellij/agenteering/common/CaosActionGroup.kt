package com.badahori.creatures.plugins.intellij.agenteering.common

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.IS_OR_HAS_CAOS_FILES_DATA_KEY
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.isOrHasCreaturesFiles
import com.bedalton.common.util.trySilent
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.runReadAction


class CaosActionGroup : DefaultCompactActionGroup(), CompactActionGroup {


    private val allActions = mutableListOf<AnAction>()

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }


    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return runReadAction {

            val event = eventWithHasCreaturesFilesContext(e)

            super.getChildren(e)
                .filter {
                    it !is ConditionalAction || it.isEnabled(event)
                }.toTypedArray()
        }
    }

    private fun eventWithHasCreaturesFilesContext(e: AnActionEvent?): AnActionEvent {
        val context = e?.dataContext

        val hasCreaturesFiles by lazy {
            e != null && isOrHasCreaturesFiles(e.files)
        }

        val dataContext = DataContext { dataId ->
            when (dataId) {
                IS_OR_HAS_CAOS_FILES_DATA_KEY.name -> hasCreaturesFiles
                else -> context?.getData(dataId)
            }
        }

        return e?.withDataContext(dataContext)
            ?: AnActionEvent.createFromDataContext(
                "CaosActionGroup",
                Presentation("CaosActionGroup"),
                dataContext
            )
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        runReadAction {
            val valid = trySilent {
                isOrHasCreaturesFiles(e.files)
            } ?: false

            updatePresentation(e) {
                isEnabledAndVisible = valid
            }
        }
    }

}