package us.n8l.duplicatefinder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Path {
    @NotNull
    String getName();

    @NotNull
    String getFullPath();

    @Nullable
    DirectoryPath getParent();
}
