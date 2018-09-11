package us.n8l.duplicatefinder.digests

import java.io.InputStream

class MD5 : BaseDigest {
    constructor(bytes: ByteArray) : super(bytes)

    constructor(inStream: InputStream) : super(inStream)

    override fun getAlgorithm() = Algorithm.MD5
}
