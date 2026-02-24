package me.mudkip.moememos.data.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.mapSuccess
import me.mudkip.moememos.data.local.dao.MemoDao
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.repository.RemoteRepository
import java.util.UUID

const val MEMOS_PAGE_SIZE = 10

class MemosPagingSource(
    private val remoteRepository: RemoteRepository,
    private val memoDao: MemoDao,
    private val accountKey: String,
    private val filter: String? = null,
    private val orderBy: String? = null,
) : PagingSource<String, MemoEntity>() {

    override fun getRefreshKey(state: PagingState<String, MemoEntity>): String? {
        return null
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, MemoEntity> {
        return try {
            val response = remoteRepository.listMemosPaged(
                pageSize = params.loadSize,
                pageToken = params.key,
                filter = filter,
                orderBy = orderBy
            )
            val pair = response.mapSuccess { this }.getOrThrow()
            val memos = pair.first
            val nextPageToken = pair.second
            val entities = memos.map { memo -> convertAndCache(memo) }
            LoadResult.Page(
                data = entities,
                prevKey = null,
                nextKey = nextPageToken,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun convertAndCache(memo: Memo): MemoEntity {
        val existing = memoDao.getMemoByRemoteId(memo.remoteId, accountKey)
        // Skip caching if local memo has pending changes
        if (existing != null && existing.needsSync) {
            val resources = memoDao.getMemoResources(existing.identifier, accountKey)
            return existing.copy().also { it.resources = resources }
        }

        val localIdentifier = existing?.identifier ?: UUID.randomUUID().toString()
        val remoteUpdatedAt = memo.updatedAt ?: memo.date
        val remoteLocation = memo.location

        val entity = MemoEntity(
            identifier = localIdentifier,
            remoteId = memo.remoteId,
            accountKey = accountKey,
            content = memo.content,
            date = memo.date,
            visibility = memo.visibility,
            pinned = memo.pinned,
            archived = memo.archived,
            needsSync = false,
            isDeleted = false,
            lastModified = remoteUpdatedAt,
            lastSyncedAt = remoteUpdatedAt,
            locationPlaceholder = remoteLocation?.placeholder,
            locationLatitude = remoteLocation?.latitude,
            locationLongitude = remoteLocation?.longitude,
            locationZoom = existing?.locationZoom ?: remoteLocation?.zoom
        )
        memoDao.insertMemo(entity)

        // Cache resources to Room and attach to entity
        val currentResources = memoDao.getMemoResources(localIdentifier, accountKey)
        val remoteResourceIds = memo.resources.mapTo(hashSetOf()) { it.remoteId }

        // Remove resources no longer present remotely
        currentResources.forEach { currentResource ->
            if (currentResource.remoteId !in remoteResourceIds) {
                memoDao.deleteResource(currentResource)
            }
        }

        // Upsert remote resources
        val cachedResources = memo.resources.map { resource ->
            val existingResource = currentResources.firstOrNull { it.remoteId == resource.remoteId }
            val localResourceId = existingResource?.identifier ?: UUID.randomUUID().toString()
            val resourceEntity = ResourceEntity(
                identifier = localResourceId,
                remoteId = resource.remoteId,
                accountKey = accountKey,
                date = resource.date,
                filename = resource.filename,
                uri = resource.uri,
                localUri = existingResource?.localUri,
                mimeType = resource.mimeType,
                memoId = localIdentifier
            )
            memoDao.insertResource(resourceEntity)
            resourceEntity
        }

        return entity.copy().also { it.resources = cachedResources }
    }
}
