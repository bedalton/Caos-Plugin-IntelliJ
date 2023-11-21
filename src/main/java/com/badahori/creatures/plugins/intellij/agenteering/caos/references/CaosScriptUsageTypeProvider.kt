package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefDocComment
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.intellij.psi.PsiElement
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProvider

class CaosScriptUsageTypeProvider: UsageTypeProvider {
    override fun getUsageType(element: PsiElement): UsageType? {

        if (element is CaosDefCompositeElement) {
            return when {
                element.hasParentOfType(CaosDefValuesListElement::class.java) -> CAOS_DEF_VALUES_LIST_USAGE_TYPE
                element.hasParentOfType(CaosDefDocComment::class.java) -> CAOS_DEF_DOC_COMMENT_USAGE_TYPE
                else -> CAOS_DEF_DECLARATION_USAGE_TYPE
            }
        }

        if (element !is CaosScriptCompositeElement) {
            return null
        }


        (element.getSelfOrParentOfType(CaosScriptClassifier::class.java))?.let { classifier ->
            return when (classifier.parent) {
                is CaosScriptScriptElement -> CAOS_EVENT_SCRIPT_USAGE_TYPE
                is CaosScriptEnumHeaderCommand -> CAOS_ENUM_USAGE_TYPE
                is CaosScriptEscnHeader -> CAOS_ENUM_USAGE_TYPE
                is CaosScriptCRtar -> UsageType.READ
                else -> null
            }
        }


        if (element.hasParentOfType(CaosScriptCaos2Block::class.java)) {
            return if (element.variant?.isOld == true) {
                CAOS2COB_USAGE_TYPE
            } else {
                CAOS2PRAY_USAGE_TYPE
            }
        }

        if (element.hasParentOfType(CaosScriptStringLike::class.java)) {
            return CAOS_STRING_USAGE_TYPE
        }

        if (element.hasParentOfType(CaosScriptRvalue::class.java)) {
            return UsageType.READ
        }

        if (element.hasParentOfType(CaosScriptLvalue::class.java)) {
            return UsageType.WRITE
        }

        if (element.hasParentOfType(CaosScriptComment::class.java)) {
            return UsageType.COMMENT_USAGE
        }
        return null
    }
}

private val CAOS_DEF_USAGE_TYPE by lazy {
    UsageType {
        CaosBundle.message("caosdef.usage-type.caosdef")
    }
}

private val CAOS_DEF_DECLARATION_USAGE_TYPE by lazy {
    UsageType {
        CaosBundle.message("caosdef.usage-type.declaration")
    }
}


private val CAOS_DEF_DOC_COMMENT_USAGE_TYPE by lazy {
    UsageType {
        CaosBundle.message("caosdef.usage-type.doc-comment")
    }
}

private val CAOS_DEF_VALUES_LIST_USAGE_TYPE by lazy {
    UsageType {
        CaosBundle.message("caosdef.usage-type.values-list")
    }
}

private val CAOS2PRAY_USAGE_TYPE by lazy {
    UsageType {
        CaosBundle.message("caos.usage-type.caos2pray")
    }
}

private val CAOS2COB_USAGE_TYPE by lazy {
    UsageType {
        CaosBundle.message("caos.usage-type.caos2cob")
    }
}


private val CAOS_STRING_USAGE_TYPE by lazy {
    UsageType {
        CaosBundle.message("caos.usage-type.string")
    }
}

private val CAOS_ENUM_USAGE_TYPE by lazy {
    UsageType {
        CaosBundle.message("caos.usage-type.enum")
    }
}

private val CAOS_EVENT_SCRIPT_USAGE_TYPE by lazy {
    UsageType {
        CaosBundle.message("caos.usage-type.script")
    }
}