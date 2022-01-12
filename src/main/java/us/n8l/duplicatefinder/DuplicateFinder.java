package us.n8l.duplicatefinder;

import org.apache.commons.lang3.StringUtils;
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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DuplicateFinder {
  private static final Set<FileVisitOption> FILE_VISIT_OPTIONS = Set.of(FileVisitOption.FOLLOW_LINKS);
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
  private final LongAdder fullFilesMatched = new LongAdder();
  private final LongAdder directoriesProcessed = new LongAdder();
  private final LongAdder failures = new LongAdder();
  private final TimeDistribution headhashTimes = new TimeDistribution();
  private final TimeDistribution fullhashTimes = new TimeDistribution();

  public DuplicateFinder(@NotNull String... directories) {
    long start = System.currentTimeMillis();
    Timer t = new Timer("Progress monitor", true);
    t.schedule(new StatusLogger(), 1_000, 5_000);
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
    List<ConcurrentLinkedQueue<FilePath>> filtered =
        filesBySize.entrySet()
            .parallelStream()
            .filter(x -> x.getKey() > 0)
            .filter(this::filterAndCountSizeGroup)
            .sorted((o1, o2) -> o2.getKey().compareTo(o1.getKey()))
            .map(Entry::getValue)
            .collect(Collectors.toUnmodifiableList());
    System.out.printf("After filtering out groups of only 1: %d\n", filtered.size());
    printSizeDistribution(filtered);
    List<ConcurrentLinkedQueue<FilePath>> headMd5Groups = filtered.parallelStream()
        .flatMap(this::getSublistsByHeadHash)
        .sorted(this::sortBySizeDescending)
        .collect(Collectors.toList());
    System.out.printf("Unique groups with headhash and size the same: %d\n", headMd5Groups.size());
    printSizeDistribution(headMd5Groups);
    List<ConcurrentLinkedQueue<FilePath>> filteredByHeadMD5 = headMd5Groups.parallelStream().filter(this::filterAndCountMatchingHeadHash).collect(Collectors.toList());
    System.out.printf("Groups with more than one match for headhash and size the same: %d\n", filteredByHeadMD5.size());
    printSizeDistribution(filteredByHeadMD5);
//    filteredByHeadMD5.forEach(System.out::println);
    List<ConcurrentLinkedQueue<FilePath>> fullHashGroups = filteredByHeadMD5.parallelStream().flatMap(this::getSublistsByFullHash).collect(Collectors.toList());
    System.out.printf("Unique groups with fullhash, headhash and size the same: %d\n", fullHashGroups.size());
//    fullHashGroups.forEach(System.out::println);
    printSizeDistribution(fullHashGroups);
    List<ConcurrentLinkedQueue<FilePath>> filteredByFullHash = fullHashGroups.parallelStream().filter(this::filterAndCountMatchingHeadHash).collect(Collectors.toList());
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
    if ((args == null) || (args.length == 0) || StringUtils.isBlank(args[0])) {
      System.err.println("Usage: duplicatefinder [directories...]");
      System.exit(-1);
    }
    new DuplicateFinder(args);
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

  private int sortBySizeDescending(ConcurrentLinkedQueue<FilePath> filePaths1, ConcurrentLinkedQueue<FilePath> filePaths2) {
    long size1 = getSizeFromFilePathQueue(filePaths1);
    long size2 = getSizeFromFilePathQueue(filePaths2);
    int compare = Long.compare(size2, size1);
    return (compare != 0) ? compare : Integer.compare(filePaths2.size(), filePaths1.size());
  }

  @NotNull
  private Long getSizeFromFilePathQueue(ConcurrentLinkedQueue<FilePath> filePaths) {
    return Optional.ofNullable(filePaths.peek()).map(FilePath::getEntry).map(FileEntry::getSize).orElse(-1L);
  }

  private boolean filterAndCountMatchingHeadHash(Collection<FilePath> x) {
    if (x.size() <= 1) {
      return false;
    } else {
      headerAndSizeMatchedFiles.add(x.size());
      return true;
    }
  }

  private boolean filterAndCountMatchingFullHash(Collection<FilePath> x) {
    if (x.size() <= 1) {
      return false;
    } else {
      fullFilesMatched.add(x.size());
      return true;
    }
  }

  private void printSizeDistribution(@Nonnull List<ConcurrentLinkedQueue<FilePath>> filtered) {
    System.out.println(
        filtered.parallelStream().collect(
            Collectors.groupingBy(
                Collection::size,
                Collectors.counting()
            )
        )
    );
  }

  private Stream<ConcurrentLinkedQueue<FilePath>> getSublistsByHeadHash(ConcurrentLinkedQueue<FilePath> candidates) {
    return getSublistsHash(candidates, this::getHeadHash);
  }

  private Stream<ConcurrentLinkedQueue<FilePath>> getSublistsByFullHash(ConcurrentLinkedQueue<FilePath> candidates) {
    return getSublistsHash(candidates, this::getFullHash);
  }

  private Stream<ConcurrentLinkedQueue<FilePath>> getSublistsHash(ConcurrentLinkedQueue<FilePath> candidates, Function<String, DigestProvider> getHash) {
    final ConcurrentMap<? super DigestProvider, ConcurrentLinkedQueue<FilePath>> cm = new ConcurrentHashMap<>();
    candidates.parallelStream().forEach(fp -> groupByFilePathHash(fp, getHash, cm));
    return cm.values().parallelStream();
  }

  private void groupByFilePathHash(@NotNull FilePath fp, @NotNull Function<String, DigestProvider> getHash, ConcurrentMap<? super DigestProvider, ConcurrentLinkedQueue<FilePath>> cm) {
    DigestProvider digest = getHash.apply(fp.getFullPath());
    if ((digest != null) && (digest.getValue() != null)) {
      Collection<FilePath> filePaths = cm.computeIfAbsent(digest, x -> new ConcurrentLinkedQueue<>());
      filePaths.add(fp);
    }
  }

  @Nullable
  private SHA256 getFullHash(String file) {
    final long startTime = System.nanoTime();
    try (FileInputStream fis = new FileInputStream(file)) {
      SHA256 sha256 = new SHA256(fis);
      fullFilesHashed.increment();
      fullhashTimes.save(System.nanoTime() - startTime);
      return sha256;
    } catch (IOException e) {
      failures.increment();
      return null;
    }
  }

  @Nullable
  private MD5 getHeadHash(String file) {
    final long startTime = System.nanoTime();
    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] bytes = new byte[4096];
      fis.readNBytes(bytes, 0, 4096);
      MD5 md5 = new MD5(bytes);
      headhashTimes.save(System.nanoTime() - startTime);
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

  private boolean filterAndCountSizeGroup(Entry<Long, ConcurrentLinkedQueue<FilePath>> x) {
    final int size = x.getValue().size();
    if (size > 1) {
      sizeMatchedFiles.add(size);
    }
    return (size > 1);
  }

  private class PathFileVisitor implements FileVisitor<Path> {
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      System.err.printf("Checking directory '%s'\n", dir);
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
      System.err.printf("Finished directory '%s'\n", dir);
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

  private class StatusLogger extends TimerTask {
    @Override
    public void run() {
      //noinspection UseOfSystemOutOrSystemErr
      System.err.printf("Dirs: %d, Files: %d, Size match: %d, Head hashes: %d, Head match: %d, Full hash: %d, Fail: %d, Full hash times: [%s] \n",
          directoriesProcessed.longValue(),
          filesFound.longValue(),
          sizeMatchedFiles.longValue(),
          headersHashed.longValue(),
          headerAndSizeMatchedFiles.longValue(),
          fullFilesHashed.longValue(),
          failures.longValue(),
          fullhashTimes.getValues());
    }
  }

  private class TimeDistribution {
    private final double MILLI_IN_NANOS = (double) TimeUnit.MILLISECONDS.toNanos(1);
    private LongAccumulator max = new LongAccumulator(Long::max, 0);
    private LongAccumulator min = new LongAccumulator(Long::min, Long.MAX_VALUE);
    private LongAdder total = new LongAdder();
    private LongAdder count = new LongAdder();
    private AtomicLong last = new AtomicLong();

    void save(Long time) {
      count.increment();
      total.add(time);
      min.accumulate(time);
      max.accumulate(time);
      last.set(time);
    }

    String getValues() {
      return String.format("Min: %.1f, Max: %.1f, Mean: %.1f, Last: %.1f", toMillis(min.longValue()), toMillis(max.longValue()), meanMillis(), toMillis(last.longValue()));
    }

    private double meanMillis() {
      if (count.longValue() == 0) return 0;
      return ((double) total.longValue() / (double) count.longValue()) / MILLI_IN_NANOS;
    }

    private double toMillis(long longValue) {
      return longValue / MILLI_IN_NANOS;
    }
  }

}
