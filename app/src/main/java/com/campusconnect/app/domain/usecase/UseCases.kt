package com.campusconnect.app.domain.usecase

import com.campusconnect.app.data.repository.NoticeRepository
import com.campusconnect.app.domain.model.Notice
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits the live list of notices from the local Room cache. */
class GetNoticesUseCase @Inject constructor(
    private val repository: NoticeRepository
) {
    operator fun invoke(): Flow<List<Notice>> = repository.observeAll()
}

/** Emits notices filtered by a category string. */
class GetNoticesByCategoryUseCase @Inject constructor(
    private val repository: NoticeRepository
) {
    operator fun invoke(category: String): Flow<List<Notice>> =
        repository.observeByCategory(category)
}

/** Emits saved notices. */
class GetSavedNoticesUseCase @Inject constructor(
    private val repository: NoticeRepository
) {
    operator fun invoke(): Flow<List<Notice>> = repository.observeSaved()
}

/** Full-text search across title and content. */
class SearchNoticesUseCase @Inject constructor(
    private val repository: NoticeRepository
) {
    operator fun invoke(query: String): Flow<List<Notice>> = repository.search(query)
}
