package me.mudkip.moememos.viewmodel

sealed class MemoFilter {
    object None : MemoFilter()
    data class Tag(val tag: String) : MemoFilter()
    data class Search(val query: String) : MemoFilter()
}
