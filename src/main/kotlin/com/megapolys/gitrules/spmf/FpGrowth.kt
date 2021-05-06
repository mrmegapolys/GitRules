package com.megapolys.gitrules.spmf

import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.FPNode
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.FPTree
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets
import ca.pfv.spmf.tools.MemoryLogger
import com.megapolys.gitrules.Commit
import java.lang.System.currentTimeMillis
import kotlin.properties.Delegates.notNull

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

    fun run(commits: Collection<Commit>): Itemsets {
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
        mapSupport: Map<String, Int?>
    ) {
        if (prefixLength == MAX_PATTERN_LENGTH) {
            return
        }

        // We will check if the FPtree contains a single path
        var singlePath = true
        // This variable is used to count the number of items in the single path
        // if there is one
        var position = 0
        // if the root has more than one child, than it is not a single path
        if (tree.root.children.size > 1) {
            singlePath = false
        } else {

            // Otherwise,
            // if the root has exactly one child, we need to recursively check childs
            // of the child to see if they also have one child
            var currentNode = tree.root.children[0]
            while (true) {
                // if the current child has more than one child, it is not a single path!
                if (currentNode.children.size > 1) {
                    singlePath = false
                    break
                }
                // otherwise, we copy the current item in the buffer and move to the child
                // the buffer will be used to store all items in the path
                fpNodeTempBuffer[position] = currentNode
                position++
                // if this node has no child, that means that this is the end of this path
                // and it is a single path, so we break
                if (currentNode.children.size == 0) {
                    break
                }
                currentNode = currentNode.children[0]
            }
        }

        // Case 1: the FPtree contains a single path
        if (singlePath) {
            // We save the path, because it is a maximal itemset
            saveAllCombinationsOfPrefixPath(fpNodeTempBuffer, position, prefix, prefixLength)
        } else {
            // For each frequent item in the header table list of the tree in reverse order.
            for (i in tree.headerList.indices.reversed()) {
                // get the item
                val item = tree.headerList[i]

                // get the item support
                val support = mapSupport[item]!!

                // Create Beta by concatening prefix Alpha by adding the current item to alpha
                prefix[prefixLength] = item

                // calculate the support of the new prefix beta
                val betaSupport = Math.min(prefixSupport, support)

                // save beta to the output file
                saveItemset(prefix, prefixLength + 1, betaSupport)
                if (prefixLength + 1 < MAX_PATTERN_LENGTH) {

                    // === (A) Construct beta's conditional pattern base ===
                    // It is a subdatabase which consists of the set of prefix paths
                    // in the FP-tree co-occuring with the prefix pattern.
                    val prefixPaths: MutableList<List<FPNode>> = ArrayList()
                    var path = tree.mapItemNodes[item]

                    // Map to count the support of items in the conditional prefix tree
                    // Key: item   Value: support
                    val mapSupportBeta: MutableMap<String, Int?> = HashMap()
                    while (path != null) {
                        // if the path is not just the root node
                        if (path.parent.itemID != null) {
                            // create the prefixpath
                            val prefixPath: MutableList<FPNode> = ArrayList()
                            // add this node.
                            prefixPath.add(path) // NOTE: we add it just to keep its support,
                            // actually it should not be part of the prefixPath

                            // ####
                            val pathCount = path.counter

                            //Recursively add all the parents of this node.
                            var parent = path.parent
                            while (parent.itemID != null) {
                                prefixPath.add(parent)

                                // FOR EACH PATTERN WE ALSO UPDATE THE ITEM SUPPORT AT THE SAME TIME
                                // if the first time we see that node id
                                if (mapSupportBeta[parent.itemID] == null) {
                                    // just add the path count
                                    mapSupportBeta[parent.itemID] = pathCount
                                } else {
                                    // otherwise, make the sum with the value already stored
                                    mapSupportBeta[parent.itemID] = mapSupportBeta[parent.itemID]!! + pathCount
                                }
                                parent = parent.parent
                            }
                            // add the path to the list of prefixpaths
                            prefixPaths.add(prefixPath)
                        }
                        // We will look for the next prefixpath
                        path = path.nodeLink
                    }

                    // (B) Construct beta's conditional FP-Tree
                    // Create the tree.
                    val treeBeta = FPTree()
                    // Add each prefixpath in the FP-tree.
                    for (prefixPath in prefixPaths) {
                        treeBeta.addPrefixPath(prefixPath, mapSupportBeta, minSupport)
                    }

                    // Mine recursively the Beta tree if the root has child(s)
                    if (treeBeta.root.children.size > 0) {

                        // Create the header list.
                        treeBeta.createHeaderList(mapSupportBeta)
                        // recursive call
                        fpGrowth(treeBeta, prefix, prefixLength + 1, betaSupport, mapSupportBeta)
                    }
                }
            }
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