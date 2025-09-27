package nz.compliscan.api.refdata;

import nz.compliscan.api.refdata.model.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RefdataCache {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private List<SanctionEntry> ofac = List.of();
    private List<PepEntry> peps = List.of();
    private volatile long lastLoadedEpochMs = 0L;

    public void replace(List<SanctionEntry> ofac, List<PepEntry> peps) {
        lock.writeLock().lock();
        try {
            this.ofac = List.copyOf(ofac);
            this.peps = List.copyOf(peps);
            this.lastLoadedEpochMs = System.currentTimeMillis();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<SanctionEntry> getOfac() {
        lock.readLock().lock();
        try {
            return ofac;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<PepEntry> getPeps() {
        lock.readLock().lock();
        try {
            return peps;
        } finally {
            lock.readLock().unlock();
        }
    }

    public long getLastLoadedEpochMs() {
        return lastLoadedEpochMs;
    }
}
