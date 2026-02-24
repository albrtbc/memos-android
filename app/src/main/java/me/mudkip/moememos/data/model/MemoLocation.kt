package me.mudkip.moememos.data.model

data class MemoLocation(
    val placeholder: String,
    val latitude: Double,
    val longitude: Double,
    val zoom: Int = DEFAULT_ZOOM
) {
    companion object {
        const val DEFAULT_ZOOM = 15
        val EMPTY = MemoLocation("", 0.0, 0.0)
    }

    val isEmpty: Boolean get() = placeholder.isEmpty() && latitude == 0.0 && longitude == 0.0
}
