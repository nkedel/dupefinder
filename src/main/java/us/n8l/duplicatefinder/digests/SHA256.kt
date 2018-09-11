package us.n8l.duplicatefinder.digests

import java.io.InputStream

class SHA256 : BaseDigest {
    constructor(bytes: ByteArray) : super(bytes)

    constructor(inStream: InputStream) : super(inStream)

    override fun getAlgorithm() = Algorithm.SHA256
}
