package io.github.t3r1jj.fcms.external.authenticated.mega

import com.github.eliux.mega.MegaSession
import com.github.eliux.mega.MegaUtils
import com.github.eliux.mega.auth.MegaAuthCredentials
import com.github.eliux.mega.cmd.AbstractMegaCmdPathHandler
import com.github.eliux.mega.error.*
import io.github.t3r1jj.fcms.external.Loggable
import io.github.t3r1jj.fcms.external.authenticated.AuthenticatedStorageTemplate
import io.github.t3r1jj.fcms.external.data.Record
import io.github.t3r1jj.fcms.external.data.RecordMeta
import io.github.t3r1jj.fcms.external.data.exception.StorageException
import io.github.t3r1jj.fcms.external.data.StorageInfo
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers
import java.io.FileInputStream
import java.lang.Exception
import java.nio.file.Paths

open class Mega(private val userName: String, private val password: String) : AuthenticatedStorageTemplate() {
    companion object : Loggable {
        init {
            ByteBuddyAgent.install()
            ByteBuddy()
                    .redefine(MegaUtils::class.java)
                    .method(ElementMatchers.named("handleResult"))
                    .intercept(MethodDelegation.to(Mega::class.java))
                    .make()
                    .load(Mega::class.java.classLoader, ClassReloadingStrategy.fromInstalledAgent())
        }

        @Suppress("unused")
        @JvmStatic
        fun handleResult(code: Int) {
            val fixedCode = -code
            when (fixedCode) {
                0 -> {
                }
                -51 -> throw MegaWrongArgumentsException()
                -52 -> throw MegaInvalidEmailException()
                -53 -> throw MegaResourceNotFoundException()
                -54 -> throw MegaInvalidStateException()
                -55 -> throw MegaInvalidTypeException()
                -56 -> throw MegaOperationNotAllowedException()
                -57 -> throw MegaLoginRequiredException()
                -58 -> throw MegaNodesNotFetchedException()
                -59 -> throw MegaUnexpectedFailureException()
                -60 -> throw MegaConfirmationRequiredException()
                else -> throw MegaUnexpectedFailureException()
            }
        }

        val logger = logger();
        const val progressRateMS: Long = 1000
    }

    private var session: MegaSession? = null

    override fun login() {
        session = try {
            val currentSession = com.github.eliux.mega.Mega.currentSession()
            if (currentSession.whoAmI() != userName) {
                currentSession.logout()
                throw MegaException("Found another user session, logging out")
            }
            currentSession
        } catch (e: MegaException) {
            try {
                MegaAuthCredentials(userName, password).login()
            } catch (e: MegaException) {
                throw StorageException("Exception during login", e)
            }
        }
    }

    override fun isLogged(): Boolean {
        return session != null
    }

    override fun doAuthenticatedUpload(record: Record): RecordMeta {
        return doAuthenticatedUpload(record, null)
    }

    override fun doAuthenticatedUpload(record: Record, progressListener: ((bytesWritten: Long) -> Unit)?): RecordMeta {
        val file = stream2file(record.data)
        val size = file.length()
        if (progressListener != null) {
            startProgressListener(record.path.split("/").last(), progressListener)
        }
        session!!.uploadFile(file.absolutePath, record.path)
                .createRemoteIfNotPresent<AbstractMegaCmdPathHandler>()
                .run()
        return RecordMeta(record.name, record.path, size)
    }

    override fun doAuthenticatedDownload(filePath: String): Record {
        return doAuthenticatedDownload(filePath, null)
    }

    override fun doAuthenticatedDownload(filePath: String, progressListener: ((bytesWritten: Long) -> Unit)?): Record {
        val tempFile = java.io.File.createTempFile(System.currentTimeMillis().toString(), null)
        tempFile.delete()
        if (progressListener != null) {
            startProgressListener(filePath, progressListener)
        }
        session!!.get(filePath)
                .setLocalPath(tempFile.absolutePath)
                .run()
        val path = Paths.get(filePath)
        return Record(path.fileName.toString(), filePath, FileInputStream(tempFile.absolutePath))
    }

    private fun startProgressListener(transferName: String, progressListener: (bytesWritten: Long) -> Unit) {
        Thread {
            var bytesTotal = 0L
            try {
                do {
                    Thread.sleep(progressRateMS)
                    val transfer = MegaCmdTransfers().call().find { t -> java.lang.String(t.sourcePath).contains(transferName) }
                    transfer!!
                    progressListener.invoke(transfer.bytesWritten)
                    bytesTotal = transfer.bytesTotal
                } while (transfer!!.progress <= 0.9999)
            } catch (e: Exception) {
                logger.error("Progress listener finished listening", e)
                progressListener.invoke(bytesTotal)
            }
        }.start()
    }

    override fun doAuthenticatedFindAll(filePath: String): List<RecordMeta> {
        session!!
        return try {
            MegaCmdRecursiveList(filePath).recursiveCall()
        } catch (notFound: MegaResourceNotFoundException) {
            emptyList()
        }
    }

    override fun doAuthenticatedDelete(meta: RecordMeta) {
        session!!.remove(meta.path).run()
    }

    override fun isPresent(filePath: String): Boolean {
        return session!!.exists(filePath)
    }

    override fun doAuthenticatedGetInfo(): StorageInfo {
        session!!
        return MegaCmdDu().call()
    }

    override fun logout() {
        session?.logout()
        session = null
    }
}