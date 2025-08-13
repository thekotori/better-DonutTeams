package eu.kotori.justTeams.util;

@FunctionalInterface
public interface CancellableTask {
    void cancel();
}