package com.megapolys.gitrules.miner.dataSource

import com.megapolys.gitrules.miner.Commit
import java.io.File

class RenameHandlingGitLogDataSource(
    private val fileName: String
) : DataSource {
    override fun getCommits() =
        File(fileName)
            .readCommitLines()
            .map { it.parseIntoFileChanges() }
            .mapToCommitsApplyingRenames()
}

private fun List<String>.parseIntoFileChanges() =
    map {
        it.split("\t").run {
            FileChange(
                changeType = get(0).first(),
                name = get(1),
                newName = getOrNull(2)
            )
        }
    }

private fun List<List<FileChange>>.mapToCommitsApplyingRenames(): List<Commit> {
    val renameMap = createRenameMap()

    return map { rawCommit ->
        rawCommit.map { renameMap[it.name] ?: it.name }
    }.map(::Commit)
}

private fun List<List<FileChange>>.createRenameMap(): Map<String, String> {
    val rawRenameMap = flatten()
        .filter(FileChange::isRename)
        .associate { it.name to it.newName!! }

    return rawRenameMap.mapValues {
        val startName = it.value
        var newName = it.value
        val seenNames = mutableSetOf(newName)

        while (true) {
            newName = rawRenameMap[newName] ?: break
            if (newName in seenNames) {
                newName = startName
                break
            }
            seenNames.add(newName)
        }
        newName
    }
}


private data class FileChange(
    val changeType: Char,
    val name: String,
    val newName: String?
) {
    fun isRename() = changeType == 'R'
}





