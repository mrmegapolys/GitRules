package com.megapolys.gitrules.spmf

const val ROOT = "ROOT"

class FpTree {
    val root = FpNode(ROOT, parent = null)
    lateinit var headerList: List<String>

    private val mapItemLastNode = mutableMapOf<String, FpNode>()
    val mapItemFirstNode = mutableMapOf<String, FpNode>()

    fun addTransaction(transaction: List<String>) {
        var currentNode = root
        transaction.forEach { itemName ->
            when (val child = currentNode.findChildWithName(itemName)) {
                null -> {
                    val newNode = FpNode(itemName, parent = currentNode)

                    currentNode.children.add(newNode)
                    currentNode = newNode

                    updateNodeLinks(newNode)
                }
                else -> {
                    child.counter++
                    currentNode = child
                }
            }
        }
    }

    fun addPrefixPath(
        prefixPath: List<FpNode>,
        supportMapBeta: Map<String, Int>,
        relativeMinSupport: Int
    ) {
        val pathCount = prefixPath[0].counter
        var currentNode = root

        for (i in prefixPath.lastIndex downTo 1) {
            val pathItem = prefixPath[i]
            if (checkNotNull(supportMapBeta[pathItem.itemName]) < relativeMinSupport) continue
            when (val child = currentNode.findChildWithName(pathItem.itemName)) {
                null -> {
                    val newNode = FpNode(pathItem.itemName, parent = currentNode)
                        .apply { counter = pathCount }

                    currentNode.children.add(newNode)
                    currentNode = newNode

                    updateNodeLinks(newNode)
                }
                else -> {
                    child.counter += pathCount
                    currentNode = child
                }
            }
        }
    }

    private fun updateNodeLinks(newNode: FpNode) {
        val itemName = newNode.itemName

        mapItemLastNode.put(itemName, newNode)
            ?.nodeLink = newNode

        mapItemFirstNode.putIfAbsent(itemName, newNode)
    }

    fun createHeaderList(supportMap: Map<String, Int>) {
        headerList = mapItemFirstNode
            .keys
            .sortDescendingBySupport(supportMap)
    }
}