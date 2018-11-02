package io.github.t3r1jj.fcms.external.authorized

import io.github.t3r1jj.fcms.external.data.RecordMeta
import io.github.t3r1jj.fcms.external.data.StorageInfo
import io.github.t3r1jj.fcms.external.upstream.CleanableStorage
import io.github.t3r1jj.fcms.external.upstream.UpstreamStorage

interface Storage : UpstreamStorage, CleanableStorage {
    /**
     * Logs in to storage service. May throw [io.github.t3r1jj.fcms.external.data.StorageException] if cannot authenticate due to wrong credentials or unexpected error.
     */
    fun login()

    /**
     * Returns true if the user is authenticated.
     */
    fun isLogged(): Boolean

    /**
     * Returns [List] of [RecordMeta] representing result of recursive search for [io.github.t3r1jj.fcms.external.data.Record] starting from [filePath].
     */
    fun findAll(filePath: String): List<RecordMeta>

    /**
     * Returns [StorageInfo]. Not all values may be correct, as some storage APIs do not provide whole info. In that case standard values for given storage may be returned.
     */
    fun getInfo(): StorageInfo

    /**
     * Logs out. Closes session if there is any.
     */
    fun logout()
}