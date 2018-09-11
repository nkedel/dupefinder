package us.n8l.duplicatefinder.digests

import org.apache.commons.codec.digest.DigestUtils
import org.jetbrains.annotations.Contract

import java.io.IOException
import java.io.InputStream
import java.util.Arrays
import java.util.Objects

abstract class BaseDigest : DigestProvider {
    private val value: ByteArray?

    internal abstract fun getAlgorithm(): Algorithm

    constructor(bytes: ByteArray) {
        value = getAlgorithm().digester().digest(bytes)
    }

    constructor(inStream: InputStream) {
        var temp: ByteArray? = null
        try {
            temp = DigestUtils.digest(getAlgorithm().digester(), inStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        value = temp
    }

    @Contract(value = "null -> false", pure = true)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseDigest) return false
        val digest = other as BaseDigest?
        return Arrays.equals(getValue(), digest!!.getValue()) && getAlgorithm() === digest.getAlgorithm()
    }

    override fun hashCode(): Int {
        var result = Objects.hash(getAlgorithm())
        result = 31 * result + Arrays.hashCode(getValue())
        return result
    }

    override fun getValue(): ByteArray {
        return value!!.clone()
    }

}

