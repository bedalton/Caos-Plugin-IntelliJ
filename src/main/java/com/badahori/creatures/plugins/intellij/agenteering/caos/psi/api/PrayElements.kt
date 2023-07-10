package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api


interface PrayTag: CaosScriptCompositeElement {
    val tagName: String
    val isNumberValue: Boolean
    val isStringValue: Boolean
    val valueAsInt: Int?
    val valueAsString: String?
}

interface PrayChildElement: CaosScriptCompositeElement

interface PrayTagName : PrayChildElement {
    val stringValue: String
}

interface PrayTagValue: PrayChildElement {
    // If value is int or float, though float is invalid
    val isNumberValue: Boolean
    // If is number and number is float, number value will be null
    val valueAsInt: Int?
    val isStringValue: Boolean
    val valueAsString: String?
}
