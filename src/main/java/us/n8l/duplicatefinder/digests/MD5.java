package us.n8l.duplicatefinder.digests;

import org.jetbrains.annotations.NotNull;

public class MD5 extends BaseDigest {
    public MD5(@NotNull byte[] bytes) {
        super(bytes);
    }

    @Override
    protected Algorithm getAlgorithm() {
        return Algorithm.MD5;
    }
}
