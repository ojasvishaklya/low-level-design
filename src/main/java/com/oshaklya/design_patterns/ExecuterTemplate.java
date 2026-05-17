package com.oshaklya.design_patterns;

import java.util.concurrent.*;

public class ExecuterTemplate {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // Example 1: Regular executor (non-daemon threads)
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> task = executor.submit(() -> {
                System.out.println("Regular thread: " + Thread.currentThread().getName() +
                                   " (daemon: " + Thread.currentThread().isDaemon() + ")");
                return 10;
            });
            Integer result = task.get();
            System.out.println("Task returned: " + result);
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            System.out.println("Executor shut down");
        }

        System.out.println("\n--- Daemon Thread with Lambda ---");
        ExecutorService daemonExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("DaemonThread-" + System.currentTimeMillis());
            return t;
        });

        daemonExecutor.submit(() -> System.out.println("Lambda daemon: " + Thread.currentThread().getName() +
                           " (daemon: " + Thread.currentThread().isDaemon() + ")"));

    }
}
