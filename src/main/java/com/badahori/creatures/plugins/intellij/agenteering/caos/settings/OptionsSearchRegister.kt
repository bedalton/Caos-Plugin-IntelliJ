package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.intellij.ide.ui.search.SearchableOptionContributor
import com.intellij.ide.ui.search.SearchableOptionProcessor


class OptionsSearchRegister: SearchableOptionContributor() {
    private val componentId = "com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosProjectSettingsConfigurable"
    private val componentName = "CAOS & Agenteering"

    override fun processOptions(processor: SearchableOptionProcessor) {
        processConfigurable(
            processor,
            SettingsSearchableStrings.strings
        )
    }


    private fun processConfigurable(
        processor: SearchableOptionProcessor,
        searchables: List<String>
    ) {
        processUILabel(processor, componentName)
        for (item in searchables) {
            processUILabel(processor, item)
        }
    }

    private fun processUILabel(
        processor: SearchableOptionProcessor,
        text: String
    ) {
        /*
         * Take text that can be found on a setting page, split it into words and add them to the internal setting search index.
         *
         * @param text                    the text that appears on a setting page and can be searched for
         * @param path                    for complex settings pages, identifies the subpage where the option is to be found.
         *                                For example, it can be the name of tab on the settings page that should be opened when showing search results.
         *                                Can be {@code null} for simple configurables.
         * @param hit                     the string that's presented to the user when showing found results in a list, e.g. in Goto Action.
         * @param configurableId          the id of the topmost configurable containing the search result. See {@link SearchableConfigurable#getId()}
         * @param configurableDisplayName display name of the configurable containing the search result
         * @param applyStemming           whether only word stems should be indexed or the full words. Porter stemmer is used.
         */
        processor.addOptions(
            /* text */ text,
            /* path */ null,
            /* hit */ null,
            /* configurableId */ componentId,
            /* configurableDisplayName */ componentName,
            /* apply Stemming */ true,
        )
    }

}