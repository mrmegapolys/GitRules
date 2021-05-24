package com.megapolys.gitrules.miner.dataSource

import com.megapolys.gitrules.miner.Commit

interface DataSource {
    fun getCommits(): List<Commit>
}