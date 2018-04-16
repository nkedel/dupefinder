package us.n8l.duplicatefinder;

import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import us.n8l.duplicatefinder.digests.MD5;
import us.n8l.duplicatefinder.digests.SHA256;

import java.util.Optional;

public class FileEntry {
    long id;
    long size; // in bytes
    MD5 headerHash;
    SHA256 hash;
    DateTime mtime;
    DateTime ctime;
    DateTime atime;
}
