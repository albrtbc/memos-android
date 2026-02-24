package me.mudkip.moememos.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import me.mudkip.moememos.data.model.MemoLocation
import me.mudkip.moememos.data.model.MemoRepresentable
import me.mudkip.moememos.data.model.MemoVisibility
import java.time.Instant

@Entity(
    tableName = "memos",
    indices = [
        Index(value = ["accountKey"]),
        Index(value = ["accountKey", "remoteId"])
    ]
)
data class MemoEntity(
    @PrimaryKey
    val identifier: String,
    override val remoteId: String? = null,
    val accountKey: String,
    override val content: String,
    override val date: Instant,
    override val visibility: MemoVisibility,
    override val pinned: Boolean,
    override val archived: Boolean = false,
    val needsSync: Boolean = true,
    val isDeleted: Boolean = false,
    val lastModified: Instant = Instant.now(),
    val lastSyncedAt: Instant? = null,
    val locationPlaceholder: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val locationZoom: Int? = null
) : MemoRepresentable {
    @Ignore
    override var resources: List<ResourceEntity> = emptyList()

    @get:Ignore
    override val location: MemoLocation?
        get() = if (locationPlaceholder != null || locationLatitude != null || locationLongitude != null) {
            MemoLocation(
                placeholder = locationPlaceholder ?: "",
                latitude = locationLatitude ?: 0.0,
                longitude = locationLongitude ?: 0.0,
                zoom = locationZoom ?: MemoLocation.DEFAULT_ZOOM
            )
        } else {
            null
        }
}
