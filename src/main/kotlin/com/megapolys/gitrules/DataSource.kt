package com.megapolys.gitrules

interface DataSource {
    fun getCommits(): Collection<Commit>
}