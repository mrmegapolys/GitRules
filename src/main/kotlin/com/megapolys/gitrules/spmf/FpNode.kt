package com.megapolys.gitrules.spmf

class FpNode(
    val itemName: String,
    val parent: FpNode?
) {
    var nodeLink: FpNode? = null
    val children = mutableListOf<FpNode>()
    var counter = 1

    fun findChildWithName(itemName: String) =
        children.find { it.itemName == itemName }
}