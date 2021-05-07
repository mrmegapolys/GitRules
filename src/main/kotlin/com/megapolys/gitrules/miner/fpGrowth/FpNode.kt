package com.megapolys.gitrules.miner.fpGrowth

class FpNode(
    val itemName: String,
    val parent: FpNode?
) {
    val children = mutableListOf<FpNode>()
    var nodeLink: FpNode? = null
    var counter = 1

    fun findChildWithName(itemName: String) =
        children.find { it.itemName == itemName }
}