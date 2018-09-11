package us.n8l.duplicatefinder;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.n8l.duplicatefinder.digests.DigestProvider;
import us.n8l.duplicatefinder.digests.MD5;
import us.n8l.duplicatefinder.digests.SHA256;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DuplicateFinder {
  private static final Set<FileVisitOption> FILE_VISIT_OPTIONS = ImmutableSet.of(FileVisitOption.FOLLOW_LINKS);
  private final Map<String, DirectoryPath> directories = new ConcurrentHashMap<>();
  private final Map<Long, ConcurrentLinkedQueue<FilePath>> filesBySize = new ConcurrentHashMap<>();
  //  private final Map<MD5, ConcurrentLinkedQueue<FilePath>> filesByHeaderHash = new ConcurrentHashMap<>();
//  private final Map<SHA256, ConcurrentLinkedQueue<FilePath>> filesByHash = new ConcurrentHashMap<>();
  private final Map<Integer, ConcurrentLinkedQueue<FilePath>> filesById = new ConcurrentHashMap<>();
  private final Map<Integer, FileEntry> entriesById = new ConcurrentHashMap<>();
  //  private final Map<String, FilePath> allFiles = new ConcurrentHashMap<>();
//  private final LongAdder filesProcessed = new LongAdder();
  private final LongAdder filesFound = new LongAdder();
  private final LongAdder sizeMatchedFiles = new LongAdder();
  private final LongAdder headersHashed = new LongAdder();
  private final LongAdder headerAndSizeMatchedFiles = new LongAdder();
  private final LongAdder fullFilesHashed = new LongAdder();
  private final LongAdder directoriesProcessed = new LongAdder();
  private final LongAdder failures = new LongAdder();

  public DuplicateFinder(@NotNull String... directories) {
    long start = System.currentTimeMillis();
    Timer t = new Timer("Progress monitor", true);
    t.schedule(new TimerTask() {
      @Override
      public void run() {
        //noinspection UseOfSystemOutOrSystemErr
        System.err.printf("Directories processed: %d, Files found %d, Sizes matched %d, Headers Hashed %d, Header matches %d, Full Files hashed %d, failures %d\n", directoriesProcessed.longValue(), filesFound.longValue(), sizeMatchedFiles.longValue(), headersHashed.longValue(), headerAndSizeMatchedFiles.longValue(), fullFilesHashed.longValue(), failures.longValue());
      }
    }, 10_000, 10_000);
    final FileVisitor<Path> visitor = new PathFileVisitor();
    for (String dir : directories) {
      Path path = Paths.get(dir);
      try {
        Files.walkFileTree(path, FILE_VISIT_OPTIONS, Integer.MAX_VALUE, visitor);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    System.out.printf("File groups by size: %d\n", filesBySize.size());
    List<Collection<FilePath>> filtered =
        filesBySize.entrySet()
            .parallelStream()
            .filter(x -> {
              final int size = x.getValue().size();
              if (size > 1) {
                sizeMatchedFiles.add(size);
              }
              return (size > 1);
            })
            .sorted((o1, o2) -> o2.getKey().compareTo(o1.getKey()))
            .map(Entry::getValue)
            .collect(Collectors.toUnmodifiableList());
    System.out.printf("After filtering out groups of only 1: %d\n", filtered.size());
    printSizeDistribution(filtered);
    List<Collection<FilePath>> md5Groups = filtered.parallelStream().flatMap(this::getSublistsByHeadHash).collect(Collectors.toList());
    System.out.printf("Unique groups with headhash and size the same: %d\n", md5Groups.size());
    printSizeDistribution(md5Groups);
    List<Collection<FilePath>> filteredByMD5 = md5Groups.parallelStream().filter(x -> x.size() > 1).collect(Collectors.toList());
    System.out.printf("Groups with more than one match for headhash and size the same: %d\n", filteredByMD5.size());
    printSizeDistribution(filteredByMD5);
//    filteredByMD5.forEach(System.out::println);
    List<Collection<FilePath>> fullHashGroups = filteredByMD5.parallelStream().flatMap(this::getSublistsByFullHash).collect(Collectors.toList());
    System.out.printf("Unique groups with fullhash, headhash and size the same: %d\n", fullHashGroups.size());
//    fullHashGroups.forEach(System.out::println);
    printSizeDistribution(fullHashGroups);
    List<Collection<FilePath>> filteredByFullHash = fullHashGroups.parallelStream().filter(x -> x.size() > 1).collect(Collectors.toList());
    printSizeDistribution(filteredByFullHash);
    System.out.printf("Groups with more than one match for fullhash and size the same: %d\n", filteredByFullHash.size());
    filteredByFullHash.forEach(System.out::println);
    long end = System.currentTimeMillis();
    System.err.println("Ran in " + (end - start) + "msec.");

//            .flatMap(ConcurrentLinkedQueue::stream)
//            .map(fp -> Pair.of(fp, getHeadHash(fp.getFullPath())))
//            .filter(x -> x.getRight() != null)
//            .collect(
//                Collectors.groupingByConcurrent(
//                    Pair::getRight,
//                    Collectors.mapping(
//                        Pair::getLeft,
//                        Collectors.toList()
//                    )
//                )
//            );
//    md5Goupings.values()
//        .stream()
//        .filter(x -> x.size() > 1)
//        .flatMap(List::stream)
//        .map(fp -> Pair.of(fp, getFullHash(fp.getFullPath())))
//        .filter(x -> x.getRight() != null)
//        .collect(
//            Collectors.groupingByConcurrent(
//                Pair::getRight,
//                Collectors.mapping(Pair::getLeft, Collectors.toList())
//            )
//        )


  }

  public static void main(String[] args) {
    new DuplicateFinder("c:/Users/Nate/");
  }

  @Nullable
  private static Integer findInode(Path path) {
    try {
      var inodeRaw = Files.getAttribute(path, "unix:ino");
      return (inodeRaw instanceof Integer) ? (Integer) inodeRaw : null;
    } catch (UnsupportedOperationException | IllegalArgumentException e) {
      // getting an inode is unsupported for this JVM or that filesystem
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private void printSizeDistribution(@Nonnull List<Collection<FilePath>> filtered) {
    System.out.println(
        filtered.parallelStream().collect(
            Collectors.groupingBy(
                Collection::size,
                Collectors.counting()
            )
        )
    );
  }

  private Stream<Collection<FilePath>> getSublistsByHeadHash(Collection<FilePath> candidates) {
    return getSublistsHash(candidates, this::getHeadHash);
  }

  private Stream<Collection<FilePath>> getSublistsByFullHash(Collection<FilePath> candidates) {
    return getSublistsHash(candidates, this::getFullHash);
  }

  private Stream<Collection<FilePath>> getSublistsHash(Collection<FilePath> candidates, Function<String, DigestProvider> getHash) {
    final ConcurrentMap<? super DigestProvider, Collection<FilePath>> cm = new ConcurrentHashMap<>();
    candidates.parallelStream().forEach(fp -> groupByFilePathHash(fp, getHash, cm));
    return cm.values().parallelStream();
  }

  private void groupByFilePathHash(@NotNull FilePath fp, @NotNull Function<String, DigestProvider> getHash, ConcurrentMap<? super DigestProvider, Collection<FilePath>> cm) {
    DigestProvider digest = getHash.apply(fp.getFullPath());
    if ((digest != null) && (digest.getValue() != null)) {
      Collection<FilePath> filePaths = cm.computeIfAbsent(digest, x -> new ConcurrentLinkedQueue<>());
      filePaths.add(fp);
    }
  }

  @Nullable
  private SHA256 getFullHash(String file) {
    try (FileInputStream fis = new FileInputStream(file)) {
      SHA256 sha256 = new SHA256(fis);
      fullFilesHashed.increment();
      return sha256;
    } catch (IOException e) {
      failures.increment();
      return null;
    }
  }

  @Nullable
  private MD5 getHeadHash(String file) {
    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] bytes = new byte[4096];
      fis.readNBytes(bytes, 0, 4096);
      MD5 md5 = new MD5(bytes);
      headersHashed.increment();
      return md5;
    } catch (IOException e) {
      failures.increment();
      return null;
    }
  }

  private void printTree() {
    System.out.println(filesBySize.keySet()
        .stream()
        .sorted()
        .map(filesBySize::get)
        .flatMap(Collection::stream)
        .map(f -> Pair.of(f.getEntry().getSize(), f.getFullPath()))
        .map(Object::toString)
        .collect(Collectors.joining("\n")));


  }

  private class PathFileVisitor implements FileVisitor<Path> {
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      getOrCreateDirectoryPaths(dir, attrs);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
      DirectoryPath parent = getOrCreateDirectoryPaths(file.getParent(), null);
      Integer inode = findInode(file);
      final FileEntry entry = (inode != null)
          ? entriesById.computeIfAbsent(inode, id -> new FileEntry(file, attrs, id))
          : new FileEntry(file, attrs, null);
      FilePath fp = new FilePath(parent, file, attrs, entry);
      entry.getId().ifPresent(id -> filesById.computeIfAbsent(id, X -> new ConcurrentLinkedQueue<>()).offer(fp));
      filesBySize.computeIfAbsent(entry.getSize(), X -> new ConcurrentLinkedQueue<>()).offer(fp);
      filesFound.increment();
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
      failures.increment();
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      directoriesProcessed.increment();
      return FileVisitResult.CONTINUE;
    }

    @Contract("null, _ -> null")
    @Nullable
    private DirectoryPath getOrCreateDirectoryPaths(Path dir, BasicFileAttributes attrs) {
      if ((dir == null) || (dir.getFileName() == null)) {
        return null;
      }
      String fullPath = dir.normalize().toAbsolutePath().toString();
      DirectoryPath parent = getOrCreateDirectoryPaths(dir.getParent(), null);
      return directories.computeIfAbsent(fullPath,
          abs -> new DirectoryPath(parent, abs, dir.getFileName().toString(), attrs));
    }
  }
}


