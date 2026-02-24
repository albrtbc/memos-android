package me.mudkip.moememos.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.mudkip.moememos.R
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.datasource.MEMOS_PAGE_SIZE
import me.mudkip.moememos.data.datasource.MemosPagingSource
import me.mudkip.moememos.data.local.dao.MemoDao
import me.mudkip.moememos.data.local.entity.MemoEntity
import me.mudkip.moememos.data.local.entity.MemoWithResources
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.DailyUsageStat
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.SyncStatus
import me.mudkip.moememos.data.service.AccountService
import me.mudkip.moememos.data.service.MemoService
import me.mudkip.moememos.ext.getErrorMessage
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.widget.WidgetUpdater
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MemosViewModel @Inject constructor(
    private val memoService: MemoService,
    private val accountService: AccountService,
    private val memoDao: MemoDao,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    var memos = mutableStateListOf<MemoEntity>()
        private set
    var tags = mutableStateListOf<String>()
        private set
    var errorMessage: String? by mutableStateOf(null)
        private set
    var matrix by mutableStateOf(DailyUsageStat.initialMatrix)
        private set

    val host: StateFlow<String?> =
        accountService.currentAccount
            .map { it?.getAccountInfo()?.host }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val syncStatus: StateFlow<SyncStatus> =
        memoService.syncStatus.stateIn(viewModelScope, SharingStarted.Eagerly, SyncStatus())

    // --- Paging state ---
    private val _currentFilter = MutableStateFlow<MemoFilter>(MemoFilter.None)
    private val _useLocalFallback = MutableStateFlow(false)
    private val _refreshSignal = MutableStateFlow(0L)

    private val debouncedFilter: Flow<MemoFilter> = _currentFilter
        .debounce { filter ->
            if (filter is MemoFilter.Search) 300L else 0L
        }
        .distinctUntilChanged()

    val pagedMemos: Flow<PagingData<MemoEntity>> = combine(
        accountService.currentAccount,
        debouncedFilter,
        _useLocalFallback,
        _refreshSignal
    ) { account, filter, useLocal, _ ->
        Triple(account, filter, useLocal)
    }.flatMapLatest { (account, filter, useLocal) ->
        if (account == null) {
            return@flatMapLatest flowOf(PagingData.empty<MemoEntity>())
        }

        val accountKey = account.accountKey()
        val isV1Remote = account is Account.MemosV1

        // Use API paging for search/tag on V1 accounts; Room paging otherwise.
        // Room paging handles pinned-first ordering correctly.
        val useApiPaging = isV1Remote && !useLocal && filter !is MemoFilter.None

        if (useApiPaging) {
            val apiFilter = buildApiFilter(filter)
            val remoteRepo = accountService.getRemoteRepository()
                ?: return@flatMapLatest flowOf(PagingData.empty<MemoEntity>())
            Pager(PagingConfig(pageSize = MEMOS_PAGE_SIZE)) {
                MemosPagingSource(
                    remoteRepository = remoteRepo,
                    memoDao = memoDao,
                    accountKey = accountKey,
                    filter = apiFilter,
                    orderBy = "display_time desc"
                )
            }.flow
        } else {
            Pager(PagingConfig(pageSize = MEMOS_PAGE_SIZE)) {
                when (filter) {
                    is MemoFilter.None -> memoDao.getPagedMemos(accountKey)
                    is MemoFilter.Tag -> memoDao.getPagedMemosByTag(accountKey, filter.tag)
                    is MemoFilter.Search -> memoDao.getPagedMemosBySearch(accountKey, filter.query)
                }
            }.flow.map { pagingData ->
                pagingData.map { memoWithResources ->
                    memoWithResources.memo.copy().also { it.resources = memoWithResources.resources }
                }
            }
        }
    }.cachedIn(viewModelScope)

    fun setFilter(filter: MemoFilter) {
        _useLocalFallback.value = false
        _currentFilter.value = filter
    }

    fun enableOfflineFallback() {
        _useLocalFallback.value = true
    }

    fun triggerPagingRefresh() {
        _refreshSignal.value = System.currentTimeMillis()
    }

    private fun buildApiFilter(filter: MemoFilter): String? {
        return when (filter) {
            is MemoFilter.None -> null
            is MemoFilter.Tag -> {
                val escaped = filter.tag.replace("\\", "\\\\").replace("\"", "\\\"")
                "tag in [\"$escaped\"]"
            }
            is MemoFilter.Search -> {
                val escaped = filter.query.replace("\\", "\\\\").replace("\"", "\\\"")
                "content.contains(\"$escaped\")"
            }
        }
    }

    // --- Existing logic (unchanged, used for heatmap/stats/widgets) ---

    init {
        snapshotFlow { memos.toList() }
            .onEach { matrix = calculateMatrix() }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            loadMemosSnapshot()

            memoService.syncStatus
                .map { it.syncing }
                .distinctUntilChanged()
                .collectLatest { syncing ->
                    if (syncing) {
                        return@collectLatest
                    }
                    memoService.memos.collectLatest { latestMemos ->
                        applyMemos(latestMemos)
                    }
                }
        }
    }

    private suspend fun loadMemosSnapshot() {
        when (val response = memoService.getRepository().listMemos()) {
            is ApiResponse.Success -> {
                applyMemos(response.data)
            }
            else -> {
                errorMessage = response.getErrorMessage()
            }
        }
    }

    suspend fun refreshLocalSnapshot() = withContext(viewModelScope.coroutineContext) {
        loadMemosSnapshot()
    }

    private fun applyMemos(latestMemos: List<MemoEntity>) {
        memos.clear()
        memos.addAll(latestMemos)
        errorMessage = null
    }

    suspend fun loadMemos(syncAfterLoad: Boolean = true) = withContext(viewModelScope.coroutineContext) {
        if (syncAfterLoad) {
            val compatibility = accountService.checkCurrentAccountSyncCompatibility(isAutomatic = true)
            if (compatibility !is AccountService.SyncCompatibility.Allowed) {
                return@withContext
            }

            val syncResult = memoService.sync(false)
            if (syncResult is ApiResponse.Success) {
                WidgetUpdater.updateWidgets(appContext)
                loadTags()
            } else {
                if (!syncResult.isAccessTokenInvalidFailure()) {
                    errorMessage = syncResult.getErrorMessage()
                }
            }
        }
    }

    suspend fun refreshMemos(allowHigherV1Version: String? = null): ManualSyncResult = withContext(viewModelScope.coroutineContext) {
        when (val compatibility = accountService.checkCurrentAccountSyncCompatibility(
            isAutomatic = false,
            allowHigherV1Version = allowHigherV1Version
        )) {
            is AccountService.SyncCompatibility.Blocked -> {
                return@withContext ManualSyncResult.Blocked(
                    compatibility.message ?: R.string.memos_supported_versions.string
                )
            }
            is AccountService.SyncCompatibility.RequiresConfirmation -> {
                return@withContext ManualSyncResult.RequiresConfirmation(
                    version = compatibility.version,
                    message = compatibility.message
                )
            }
            AccountService.SyncCompatibility.Allowed -> Unit
        }

        val syncResult = memoService.sync(true)
        if (syncResult is ApiResponse.Success) {
            if (allowHigherV1Version != null) {
                accountService.rememberAcceptedUnsupportedSyncVersion(allowHigherV1Version)
            }
            WidgetUpdater.updateWidgets(appContext)
            loadTags()
        } else {
            val message = syncResult.getErrorMessage()
            errorMessage = message
            return@withContext ManualSyncResult.Failed(message)
        }
        ManualSyncResult.Completed
    }

    private fun ApiResponse<Unit>.isAccessTokenInvalidFailure(): Boolean {
        return this is ApiResponse.Failure.Exception && this.throwable == MoeMemosException.accessTokenInvalid
    }

    fun loadTags() = viewModelScope.launch {
        memoService.getRepository().listTags().suspendOnSuccess {
            tags.clear()
            tags.addAll(data)
        }
    }

    suspend fun updateMemoPinned(memoIdentifier: String, pinned: Boolean) = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().updateMemo(memoIdentifier, pinned = pinned).suspendOnSuccess {
            updateMemo(data)
            WidgetUpdater.updateWidgets(appContext)
            triggerPagingRefresh()
        }
    }

    suspend fun editMemo(memoIdentifier: String, content: String, resourceList: List<ResourceEntity>?, visibility: MemoVisibility): ApiResponse<MemoEntity> = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().updateMemo(memoIdentifier, content, resourceList, visibility).suspendOnSuccess {
            updateMemo(data)
            WidgetUpdater.updateWidgets(appContext)
            triggerPagingRefresh()
        }
    }

    suspend fun archiveMemo(memoIdentifier: String) = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().archiveMemo(memoIdentifier).suspendOnSuccess {
            memos.removeIf { it.identifier == memoIdentifier }
            WidgetUpdater.updateWidgets(appContext)
            triggerPagingRefresh()
        }
    }

    suspend fun deleteMemo(memoIdentifier: String) = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().deleteMemo(memoIdentifier).suspendOnSuccess {
            memos.removeIf { it.identifier == memoIdentifier }
            WidgetUpdater.updateWidgets(appContext)
            triggerPagingRefresh()
        }
    }

    suspend fun cacheResourceFile(resourceIdentifier: String, downloadedUri: Uri): ApiResponse<Unit> = withContext(viewModelScope.coroutineContext) {
        memoService.getRepository().cacheResourceFile(resourceIdentifier, downloadedUri)
    }

    suspend fun getResourceById(resourceIdentifier: String): ResourceEntity? = withContext(viewModelScope.coroutineContext) {
        when (val response = memoService.getRepository().listResources()) {
            is ApiResponse.Success -> response.data.firstOrNull { it.identifier == resourceIdentifier }
            else -> null
        }
    }

    private fun updateMemo(memo: MemoEntity) {
        val index = memos.indexOfFirst { it.identifier == memo.identifier }
        if (index != -1) {
            memos[index] = memo
        }
    }

    private fun calculateMatrix(): List<DailyUsageStat> {
        val countMap = HashMap<LocalDate, Int>()

        for (memo in memos) {
            val date = memo.date.atZone(OffsetDateTime.now().offset).toLocalDate()
            countMap[date] = (countMap[date] ?: 0) + 1
        }

        return DailyUsageStat.initialMatrix.map {
            it.copy(count = countMap[it.date] ?: 0)
        }
    }
}

val LocalMemos =
    compositionLocalOf<MemosViewModel> { error(me.mudkip.moememos.R.string.memos_view_model_not_found.string) }

sealed class ManualSyncResult {
    object Completed : ManualSyncResult()
    data class Blocked(val message: String) : ManualSyncResult()
    data class RequiresConfirmation(val version: String, val message: String) : ManualSyncResult()
    data class Failed(val message: String) : ManualSyncResult()
}
