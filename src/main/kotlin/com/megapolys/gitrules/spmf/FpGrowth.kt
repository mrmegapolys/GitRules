package com.megapolys.gitrules.spmf

import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.FPNode
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.FPTree
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets
import ca.pfv.spmf.tools.MemoryLogger
import com.megapolys.gitrules.Commit
import java.lang.System.currentTimeMillis
import kotlin.math.min

private const val MAX_PATTERN_LENGTH = 1000
private const val BUFFERS_SIZE = 2000

private const val PREFIX_LENGTH_WARNING = "Got to max pattern length: $MAX_PATTERN_LENGTH"

class FpGrowth(private val minSupport: Int) {
    private val itemsets = Itemsets()

    private val itemsetBuffer = arrayOfNulls<String>(BUFFERS_SIZE)
    private val fpNodeTempBuffer = arrayOfNulls<FPNode>(BUFFERS_SIZE)

    fun runWithStatistics(commits: Collection<Commit>): Itemsets {
        val startTimestamp = currentTimeMillis()
        MemoryLogger.getInstance().apply {
            reset()
            checkMemory()
        }

        val itemsets = run(commits)

        val endTimestamp = currentTimeMillis()
        MemoryLogger.getInstance().checkMemory()

        println(
            """
            ================  MINER STATISTICS ================
            Transactions count from database : ${commits.size}
            Frequent itemsets count : ${itemsets.count()}
            Max memory usage: ${MemoryLogger.getInstance().maxMemory} mb 
            Total time ~ ${endTimestamp - startTimestamp} ms
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

    private fun fpGrowth(
        tree: FPTree,
        prefix: Array<String?>,
        prefixLength: Int,
        prefixSupport: Int,
        supportMap: Map<String, Int>
    ) {
        if (prefixLength == MAX_PATTERN_LENGTH) {
            println(PREFIX_LENGTH_WARNING)
            return
        }

        val singlePathLength = calculateSinglePathLength(tree)

        if (singlePathLength > 0) {
            saveAllCombinationsOfPrefixPath(fpNodeTempBuffer, singlePathLength, prefix, prefixLength)
            return
        }

        for (i in tree.headerList.indices.reversed()) {
            val item = tree.headerList[i]

            val support = checkNotNull(supportMap[item])

            prefix[prefixLength] = item
            val betaSupport = min(prefixSupport, support)
            saveItemset(prefix, prefixLength + 1, betaSupport)

            if (prefixLength + 1 >= MAX_PATTERN_LENGTH) {
                println(PREFIX_LENGTH_WARNING)
                continue
            }

            val prefixPaths = mutableListOf<List<FPNode>>()
            var path = tree.mapItemNodes[item]

            val supportMapBeta = mutableMapOf<String, Int>()

            while (path != null) {
                if (path.parent.itemID == null) {
                    path = path.nodeLink
                    continue
                }

                val prefixPath = mutableListOf(path)
                val pathCount = path.counter

                var parent = path.parent
                while (parent.itemID != null) {
                    prefixPath.add(parent)

                    supportMapBeta[parent.itemID] =
                        supportMapBeta.getOrZero(parent.itemID) + pathCount

                    parent = parent.parent
                }
                prefixPaths.add(prefixPath)
                path = path.nodeLink
            }

            val treeBeta = FPTree().apply {
                prefixPaths.forEach { addPrefixPath(it, supportMapBeta, minSupport) }
            }
            if (treeBeta.root.children.size > 0) {
                treeBeta.createHeaderList(supportMapBeta)
                fpGrowth(treeBeta, prefix, prefixLength + 1, betaSupport, supportMapBeta)
            }
        }
    }

    private fun calculateSinglePathLength(tree: FPTree): Int {
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

    private fun createSupportMap(commits: Collection<Commit>) =
        commits
            .flatMap(Commit::files)
            .groupingBy { it }
            .eachCount()

    private fun createFpTree(
        commits: Collection<Commit>,
        supportMap: Map<String, Int>
    ) = FPTree().apply {
        commits.map { commit ->
            commit.files
                .filter { supportMap.getOrZero(it) >= minSupport }
                .sortDescendingBySupport(supportMap)
        }.forEach { addTransaction(it) }
    }

    private fun saveItemset(itemsetArray: Array<String?>, itemsetLength: Int, support: Int) {
        val files = itemsetArray
            .copyOfRange(0, itemsetLength)
            .toList()
            .map { it as String }
            .sorted()

        val itemset = Itemset(files, support)
        itemsets.addItemset(itemset, itemsetLength)
    }

    private fun saveAllCombinationsOfPrefixPath(
        fpNodeTempBuffer: Array<FPNode?>,
        position: Int,
        prefix: Array<String?>,
        prefixLength: Int
    ) {
        var support = 0

        loop@ for (i in 1 until (1 shl position)) {
            var newPrefixLength = prefixLength

            for (j in 0 until position) {
                if (i and (1 shl j) <= 0) continue
                if (newPrefixLength == MAX_PATTERN_LENGTH) {
                    println(PREFIX_LENGTH_WARNING)
                    continue@loop
                }

                with(checkNotNull(fpNodeTempBuffer[j])) {
                    prefix[newPrefixLength++] = itemID
                    support = counter
                }
            }
            saveItemset(prefix, newPrefixLength, support)
        }
    }
}