package us.n8l.duplicatefinder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FilePath implements Path {
    DirectoryPath parent;
    String name;
    String fullPath;
    FileEntry file;

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public String getFullPath() {
        return fullPath;
    }

    @Nullable
    @Override
    public DirectoryPath getParent() {
        return parent;
    }
}
