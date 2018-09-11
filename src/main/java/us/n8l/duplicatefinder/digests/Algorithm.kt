package us.n8l.duplicatefinder.digests

import java.security.MessageDigest
import java.util.function.Supplier

internal enum class Algorithm(algorithmName: String) {
    MD5("MD5"),
    SHA1("SHA-1"),
    SHA256("SHA-256");

    private val digestSupplier: Supplier<MessageDigest>

    fun digester(): MessageDigest {
        return digestSupplier.get()
    }

    init {
        digestSupplier = MessageDigestSupplier(algorithmName)
    }

    private class MessageDigestSupplier internal constructor(private val algorithmName: String) : Supplier<MessageDigest> {

        override fun get(): MessageDigest {
            return MessageDigest.getInstance(algorithmName)
        }
    }
}
