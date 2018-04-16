package us.n8l.duplicatefinder;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class DuplicateFinder {
    Executor jobRunner;

    DuplicateFinder(String path, int threads) {
        jobRunner = Executors.newFixedThreadPool(threads);
    }
}
