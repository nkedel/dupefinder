package us.n8l.duplicatefinder.digests;

import org.jetbrains.annotations.NotNull;

public class SHA256 extends BaseDigest {
    public SHA256(@NotNull byte[] bytes) {
        super(bytes);
    }

    @Override
    protected Algorithm getAlgorithm() {
        return Algorithm.SHA256;
    }
}
