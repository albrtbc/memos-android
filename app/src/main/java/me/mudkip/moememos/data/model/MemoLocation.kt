package me.mudkip.moememos.data.model

data class MemoLocation(
    val placeholder: String,
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        val EMPTY = MemoLocation("", 0.0, 0.0)
    }

    val isEmpty: Boolean get() = placeholder.isEmpty() && latitude == 0.0 && longitude == 0.0
}
