package us.n8l.duplicatefinder.digests;

import org.jetbrains.annotations.NotNull;

public class SHA1 extends BaseDigest {
    SHA1(@NotNull byte[] bytes) {
        super(bytes);
    }

    @Override
    protected Algorithm getAlgorithm() {
        return Algorithm.MD5;
    }
}
