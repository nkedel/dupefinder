package us.n8l.duplicatefinder.digests;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

abstract class BaseDigest {
    private final byte[] value;

    protected abstract Algorithm getAlgorithm();

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseDigest)) return false;
        BaseDigest digest = (BaseDigest) o;
        return Arrays.equals(getValue(), digest.getValue()) &&
                (getAlgorithm() == digest.getAlgorithm());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getAlgorithm());
        result = (31 * result) + Arrays.hashCode(getValue());
        return result;
    }

    BaseDigest(@NotNull byte[] bytes) {
        value = getAlgorithm().digester.digest(bytes);
    }

    public byte[] getValue() {
        return value.clone();
    }

    enum Algorithm {
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA256("SHA-256");

        private final MessageDigest digester;

        Algorithm(String algorithmName) {
            MessageDigest toFind = null;
            try {
                toFind = MessageDigest.getInstance(algorithmName);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            digester = toFind;
        }
    }
}
