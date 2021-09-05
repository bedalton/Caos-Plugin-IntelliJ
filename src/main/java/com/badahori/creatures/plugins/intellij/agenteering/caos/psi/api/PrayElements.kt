package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api


interface PrayTag: CaosScriptCompositeElement {
    val tagName: String
    val isNumberValue: Boolean
    val isStringValue: Boolean
    val valueAsInt: Int?
    val valueAsString: String?
}

interface PrayTagName : CaosScriptCompositeElement {
    val stringValue: String
}

interface PrayTagValue: CaosScriptCompositeElement {
    // If value is int or float, though float is invalid
    val isNumberValue: Boolean
    // If is number and number is float, number value will be null
    val valueAsInt: Int?
    val isStringValue: Boolean
    val valueAsString: String?
}