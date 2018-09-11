package us.n8l.duplicatefinder.digests

import java.io.InputStream

class SHA1 : BaseDigest {
    internal constructor(bytes: ByteArray) : super(bytes)

    internal constructor(inStrean: InputStream) : super(inStrean)

    override fun getAlgorithm() = Algorithm.MD5
}
