package com.megapolys.gitrules.miner.fpGrowth

import com.megapolys.gitrules.miner.Commit
import com.megapolys.gitrules.model.Itemset
import java.lang.System.currentTimeMillis
import kotlin.math.min

private const val MAX_PATTERN_LENGTH = 5
private const val BUFFERS_SIZE = 2000

class FpGrowth(private val minSupport: Int) {
    private val itemsets = Itemsets()

    private val itemsetBuffer = arrayOfNulls<String>(BUFFERS_SIZE)
    private val fpNodeTempBuffer = arrayOfNulls<FpNode>(BUFFERS_SIZE)

    fun runWithStatistics(commits: Collection<Commit>): Itemsets {
        val startTimestamp = currentTimeMillis()
        MemoryLogger.run {
            reset()
            checkMemory()
        }

        val itemsets = run(commits)

        val endTimestamp = currentTimeMillis()
        MemoryLogger.checkMemory()

        println(
            """
            ================  MINER STATISTICS ================
            Transactions count: ${commits.size}
            Frequent itemsets count: ${itemsets.count}
            Max memory usage: ${MemoryLogger.maxMemory} mb 
            Total time: ${endTimestamp - startTimestamp} ms
            ===================================================
            """.trimIndent()
        )
        return itemsets
    }

    private fun run(commits: Collection<Commit>): Itemsets {
        val supportMap = createSupportMap(commits)
        val tree = createFpTree(commits, supportMap)
            .apply { createHeaderList(supportMap) }

        if (tree.headerList.isNotEmpty()) {
            fpGrowth(tree, itemsetBuffer, 0, commits.size, supportMap)
        }

        return itemsets
    }

    private fun createSupportMap(commits: Collection<Commit>) =
        commits
            .flatMap(Commit::files)
            .groupingBy { it }
            .eachCount()

    private fun createFpTree(
        commits: Collection<Commit>,
        supportMap: Map<String, Int>
    ) = FpTree().apply {
        commits.map { commit ->
            commit.files
                .filter { supportMap.getOrZero(it) >= minSupport }
                .sortDescendingBySupport(supportMap)
        }.forEach { addTransaction(it) }
    }

    private fun fpGrowth(
        tree: FpTree,
        prefix: Array<String?>,
        prefixLength: Int,
        prefixSupport: Int,
        supportMap: Map<String, Int>
    ) {
        if (prefixLength == MAX_PATTERN_LENGTH) return

        val singlePathLength = calculateSinglePathLength(tree)
        if (singlePathLength > 0) {
            saveAllCombinationsOfPrefixPath(fpNodeTempBuffer, singlePathLength, prefix, prefixLength)
            return
        }

        for (item in tree.headerList.reversed()) {
            val itemSupport = checkNotNull(supportMap[item])

            prefix[prefixLength] = item
            val betaSupport = min(prefixSupport, itemSupport)
            saveItemset(prefix, prefixLength + 1, betaSupport)

            if (prefixLength + 1 >= MAX_PATTERN_LENGTH) continue

            val prefixPaths = mutableListOf<List<FpNode>>()
            var path = tree.mapItemFirstNode[item]

            val supportMapBeta = mutableMapOf<String, Int>()

            while (path != null) {
                if (path.parent?.itemName == ROOT) {
                    path = path.nodeLink
                    continue
                }

                val prefixPath = mutableListOf(path)
                val pathCount = path.counter

                var parent = checkNotNull(path.parent)
                while (parent.itemName != ROOT) {
                    prefixPath.add(parent)

                    supportMapBeta[parent.itemName] =
                        supportMapBeta.getOrZero(parent.itemName) + pathCount

                    parent = checkNotNull(parent.parent)
                }
                prefixPaths.add(prefixPath)
                path = path.nodeLink
            }

            val treeBeta = FpTree().apply {
                prefixPaths.forEach { addPrefixPath(it, supportMapBeta, minSupport) }
            }
            if (treeBeta.root.children.isNotEmpty()) {
                treeBeta.createHeaderList(supportMapBeta)
                fpGrowth(treeBeta, prefix, prefixLength + 1, betaSupport, supportMapBeta)
            }
        }
    }

    private fun calculateSinglePathLength(tree: FpTree): Int {
        if (tree.root.children.size > 1) return 0

        var position = 0
        var currentNode = tree.root.children[0]
        while (true) {
            if (currentNode.children.size > 1) return 0

            fpNodeTempBuffer[position++] = currentNode

            if (currentNode.children.isEmpty()) return position
            currentNode = currentNode.children[0]
        }
    }

    private fun saveAllCombinationsOfPrefixPath(
        fpNodeTempBuffer: Array<FpNode?>,
        position: Int,
        prefix: Array<String?>,
        prefixLength: Int
    ) {
        var support = 0

        loop@ for (i in 1 until (1L shl position)) {
            var newPrefixLength = prefixLength

            for (j in 0 until position) {
                if (newPrefixLength == MAX_PATTERN_LENGTH) continue@loop
                if (i and (1L shl j) <= 0) continue

                with(checkNotNull(fpNodeTempBuffer[j])) {
                    prefix[newPrefixLength++] = itemName
                    support = counter
                }
            }
            saveItemset(prefix, newPrefixLength, support)
        }
    }

    private fun saveItemset(filesArray: Array<String?>, itemsetLength: Int, support: Int) {
        val files = filesArray
            .copyOfRange(0, itemsetLength)
            .map { it as String }
            .sorted()

        itemsets.addItemset(
            itemset = Itemset(files, support),
            level = itemsetLength
        )
    }
}
