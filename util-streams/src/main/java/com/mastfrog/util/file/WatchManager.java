/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.util.file;

import com.mastfrog.function.state.Bool;
import com.mastfrog.function.state.Int;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Manages file system watches, centralizing management of watch keys, and
 * enabling and disabling notifications; uses a single thread and a set of
 * polling intervals, and timeouts to ensure all watches are fairly serviced
 * with fixed overhead.
 *
 * @author Tim Boudreau
 */
public final class WatchManager {

    private final Map<FileSystem, FileSystemRegistration> fsRegs = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor;
    private final long perKeyWait;
    private final long maxTimePer;
    private final AtomicBoolean started = new AtomicBoolean();
    private final long interval;
    private Future<?> nextScheduled;
    private final AtomicBoolean paused = new AtomicBoolean();

    private static Reference<WatchManager> sharedInstance;

    static synchronized WatchManager sharedInstance(boolean create) {
        WatchManager result = null;
        if (sharedInstance != null) {
            result = sharedInstance.get();
        }
        if (result == null && create) {
            result = new WatchManager();
            sharedInstance = new WeakReference<>(result);
        }
        return result;
    }

    static WatchManager sharedInstance() {
        return sharedInstance(true);
    }

    /**
     * Create a watch manager.
     *
     * @param executor The executor to poll and invoke callbacks in
     * @param perKeyWait The amount of time to wait for events on each key - if
     * zero, polling will continue with no delay
     * @param maxTimePer The amount of time to spend on any given key before
     * moving on to the next one, to avoid favoring one busy folder
     * @param interval The interval between polling runs (starts with the end of
     * the previous polling run, not including the time that took)
     */
    public WatchManager(ScheduledExecutorService executor, Duration perKeyWait,
            Duration maxTimePer, Duration interval) {
        this(executor, perKeyWait.toMillis(), maxTimePer.toMillis(), interval.toMillis());
    }

    public WatchManager(Duration perKeyWait,
            Duration maxTimePer, Duration interval) {
        this(Executors.newScheduledThreadPool(1), perKeyWait, maxTimePer, interval);
    }

    public WatchManager() {
        this(Executors.newScheduledThreadPool(1), 200, 120, 500);
    }

    public WatchManager(ScheduledExecutorService executor, long perKeyWait, long maxTimePer, long interval) {
        this.executor = executor;
        this.perKeyWait = perKeyWait;
        this.maxTimePer = maxTimePer;
        this.interval = interval;
    }

    private Boolean isSharedInstance;

    private boolean isSharedInstance() {
        if (isSharedInstance != null) {
            return isSharedInstance;
        }
        WatchManager shared = sharedInstance(false);
        return isSharedInstance = (shared == this);
    }

    /**
     * Pause notification of file changes - no polling runs will occur; if a
     * polling run is currently occurring, it will complete.
     *
     * @param pause If true, pause
     * @return if the paused state changed
     */
    public boolean pause(boolean pause) {
        if (isSharedInstance()) {
            return false;
        }
        if (paused.compareAndSet(!pause, pause)) {
            synchronized (this) {
                if (pause) {
                    if (nextScheduled != null) {
                        nextScheduled.cancel(false);
                    }
                } else {
                    if (nextScheduled == null || nextScheduled.isDone() && started.get() && !isEmpty()) {
                        nextScheduled = executor.schedule(this::run, interval,
                                TimeUnit.MILLISECONDS);
                    }
                }
            }
            return true;
        }
        return false;
    }

    int pollLoop() throws InterruptedException {
        Bool anyTimedOut = Bool.create();
        int total = 0;
        List<FileSystemRegistration> copy;
        synchronized (this) {
            copy = new ArrayList<>(fsRegs.values());
        }
        for (FileSystemRegistration fs : copy) {
            long wait = anyTimedOut.getAsBoolean() ? 0 : perKeyWait;
            total += fs.pollLoop(wait, maxTimePer, anyTimedOut);
        }
        return total;
    }

    /**
     * Shut down this WatchManager, closing all watch keys.
     *
     * @return true if the state was changed
     */
    public boolean shutdown() {
        if (isSharedInstance()) {
            return false;
        }
        if (started.compareAndSet(true, false)) {
            synchronized (this) {
                if (nextScheduled != null) {
                    nextScheduled.cancel(true);
                }
                fsRegs.clear();
            }
            return true;
        }
        return false;
    }

    void run() {
        try {
            while (pollLoop() > 0);
        } catch (InterruptedException ex) {
            Logger.getLogger(WatchManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (started.get() && !paused.get()) {
                synchronized (this) {
                    if (!isEmpty()) {
                        nextScheduled = executor.schedule(this::run, interval,
                                TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }

    /**
     * Watch a file or folder; if the passed target is a file, only
     * notifications for that file will be delivered; if a folder, notifications
     * for any file or folder within that directory (folders may notify they
     * were modified when a file is created or changes within them) will be
     * delivered.
     * <p>
     * Note that the consumer is <i>weakly referenced</i> in order that watch
     * keys can be cleaned up if the listener is garbage collected.
     * </p>
     *
     * @param target The target file or folder
     * @param c The consumer
     * @param oneKind The first kind
     * @param more Additional kinds
     * @throws IOException If something goes wrong
     */
    public synchronized void watch(Path target, BiConsumer<Path, WatchEvent.Kind<?>> c,
            WatchEvent.Kind<?> oneKind, WatchEvent.Kind<?>... more) throws IOException {
        Set<WatchEvent.Kind<?>> set = new HashSet<>(more.length + 1);
        set.add(oneKind);
        set.addAll(Arrays.asList(more));
        watch(target, c, set);
    }

    /**
     * Watch a file or folder; if the passed target is a file, only
     * notifications for that file will be delivered; if a folder, notifications
     * for any file or folder within that directory (folders may notify they
     * were modified when a file is created or changes within them) will be
     * delivered.
     * <p>
     * Note that the consumer is <i>weakly referenced</i> in order that watch
     * keys can be cleaned up if the listener is garbage collected.
     * </p>
     *
     * @param target The target file or folder
     * @param c The consumer
     * @param kinds The kinds to listen to (must be non empty)
     * @throws IOException If something goes wrong
     */
    public synchronized void watch(Path target, BiConsumer<Path, WatchEvent.Kind<?>> c,
            Set<WatchEvent.Kind<?>> kinds) throws IOException {
        if (kinds.isEmpty()) {
            throw new IllegalArgumentException("No kinds");
        }
        Path folder = Files.isDirectory(target) ? target : target.getParent();
        FileSystem fs = folder.getFileSystem();
        FileSystemRegistration reg = fsRegs.computeIfAbsent(fs, FileSystemRegistration::new);
        reg.add(folder, target, kinds, c);
        if (started.compareAndSet(false, true) && !paused.get()) {
            if (!isEmpty()) {
                nextScheduled = executor.submit(this::run);
            }
        }
    }

    /**
     * Stop watching a file or folder; if the passed consumer is null, this will
     * unwatch for all listeners on the passed file.
     *
     * @param target The file or folder
     * @param c Optionally, a specific consumer to detach
     */
    public synchronized void unwatch(Path target, BiConsumer<Path, WatchEvent.Kind<?>> c) {
        // Note: With all of these remove and isEmpty methods, we cannot
        // necessarily determine if
        Set<FileSystem> toRemove = new HashSet<>();
        fsRegs.forEach((fs, reg) -> {
            if (reg.remove(target, c)) {
                toRemove.add(fs);
            }
        });
        toRemove.forEach(fsRegs::remove);
        if (isEmpty()) {
            if (nextScheduled != null) {
                nextScheduled.cancel(true);
            }
        }
    }

    /**
     * Determine if there are no active listeners on any files.
     *
     * @return true if there are none
     */
    public synchronized boolean isEmpty() {
        Set<FileSystem> toRemove = new HashSet<>();
        boolean result = true;
        for (Map.Entry<FileSystem, FileSystemRegistration> e : fsRegs.entrySet()) {
            boolean empty = e.getValue().isEmpty();
            result &= empty;
            if (empty) {
                toRemove.add(e.getKey());
                e.getValue().close();
            }
        }
        toRemove.forEach(fsRegs::remove);
        result = fsRegs.isEmpty();
        if (result) {
            started.set(false);
        }
        return result;
    }

    @Override
    public String toString() {
        Map<FileSystem, FileSystemRegistration> copy;
        synchronized (this) {
            copy = new HashMap<>(fsRegs);
        }
        StringBuilder sb = new StringBuilder("WatchManager:");
        if (copy.isEmpty()) {
            sb.append(" (empty)");
        }
        for (Map.Entry<FileSystem, FileSystemRegistration> e : copy.entrySet()) {
            for (Path p : e.getKey().getRootDirectories()) {
                sb.append(p).append(' ');
            }
            sb.append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * Allows for recursive watching on folders and files.
     */
    public interface RecursiveWatcher {

        Set<? extends Path> attach();

        Set<? extends Path> detach();
    }

    /**
     * Create a new recursive watcher, already attached.
     *
     * @param root The root folder
     * @param maxDepth The depth to which to listen for file changes (keep as
     * small as possible)
     * @param consumer The consumer
     * @param kind The first kind of event to watch for
     * @param more Other kinds of events to watch for
     * @return A new watcher
     * @throws IOException
     */
    public RecursiveWatcher recursiveWatch(Path root, int maxDepth, BiConsumer<Path, WatchEvent.Kind<?>> consumer, WatchEvent.Kind<?> kind, WatchEvent.Kind<?>... more) throws IOException {
        Set<WatchEvent.Kind<?>> all = new HashSet<>();
        all.add(kind);
        all.addAll(Arrays.asList(more));
        RecursiveWatcherImpl watcher = new RecursiveWatcherImpl(root, this, maxDepth, all, consumer);
        watcher.attach();
        return watcher;
    }

    private static final class RecursiveWatcherImpl implements BiConsumer<Path, WatchEvent.Kind<?>>, RecursiveWatcher {

        private final Path root;
        private final WatchManager mgr;
        private final Set<WatchEvent.Kind<?>> set;
        private final BiConsumer<Path, WatchEvent.Kind<?>> consumer;

        static final Set<WatchEvent.Kind<?>> ALL;
        private final Set<Path> listeningTo = ConcurrentHashMap.newKeySet(36);

        static {
            Set<WatchEvent.Kind<?>> kinds = new HashSet<>();
            kinds.add(StandardWatchEventKinds.ENTRY_CREATE);
            kinds.add(StandardWatchEventKinds.ENTRY_MODIFY);
            kinds.add(StandardWatchEventKinds.ENTRY_DELETE);
            ALL = Collections.unmodifiableSet(kinds);
        }
        private final int maxDepth;

        @SuppressWarnings("LeakingThisInConstructor")
        RecursiveWatcherImpl(Path root, WatchManager mgr, int maxDepth, Set<WatchEvent.Kind<?>> set,
                BiConsumer<Path, WatchEvent.Kind<?>> consumer) throws IOException {
            this.root = root;
            this.mgr = mgr;
            this.set = set;
            this.consumer = consumer;
            this.maxDepth = maxDepth;
        }

        @Override
        public String toString() {
            return "RecursiveWatcher(" + consumer + ")";
        }

        public synchronized Set<? extends Path> attach() {
            if (!listeningTo.isEmpty()) {
                return Collections.unmodifiableSet(listeningTo);
            }
            try {
                mgr.watch(root, this, ALL);
                listeningTo.add(root);
                try (Stream<Path> str = Files.walk(root, maxDepth, FileVisitOption.FOLLOW_LINKS)) {
                    str.filter(fl -> Files.isDirectory(fl)).forEach(pth -> {
                        try {
                            mgr.watch(pth, this, ALL);
                            listeningTo.add(pth);
                        } catch (IOException ex) {
                            Logger.getLogger(WatchManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
                }
            } catch (IOException ex) {
                Logger.getLogger(WatchManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            return Collections.unmodifiableSet(listeningTo);
        }

        public synchronized Set<Path> detach() {
            Set<Path> all = new HashSet<>(listeningTo);
            listeningTo.clear();
            all.forEach(p -> {
                mgr.unwatch(p, this);
            });
            return all;
        }

        @Override
        public void accept(Path t, WatchEvent.Kind<?> u) {
            if (u.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                if (Files.isDirectory(t)) {
                    try {
                        listeningTo.add(t);
                        mgr.watch(t, this, ALL);
                    } catch (IOException ex) {
                        Logger.getLogger(WatchManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if (set.contains(u)) {
                consumer.accept(t, u);
            }
        }
    }

    private static class FileSystemRegistration {

        private final FileSystem fs;
        private final Map<Path, FolderWatchRegistration> registrations = new HashMap<>();
        private WatchService svc;

        public FileSystemRegistration(FileSystem fs) {
            this.fs = fs;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Map<Path, FolderWatchRegistration> copy;
            synchronized (this) {
                copy = new HashMap<>(registrations);
            }
            for (Map.Entry<Path, FolderWatchRegistration> e : copy.entrySet()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append('\t').append(e.getKey()).append(' ').append(e.getValue());
            }
            return sb.toString();
        }

        synchronized boolean remove(Path path, BiConsumer<Path, WatchEvent.Kind<?>> consumer) {
            // Note: With all of these remove and isEmpty methods, we cannot
            // necessarily determine if the path is a file or a folder, because
            // it may no longer exist anymore, in which case, Files.isDirectory() is
            // useless.  So the only safe way to ensure we close unneeded watch
            // handles is to iterate the entries, find the right one based on the
            // path it was registered against, and remove that one; then GC so
            // we eliminate any dead entries and shut down any stale WatchKeys for
            // those too
            Set<Path> toRemove = new HashSet<>();
            registrations.forEach((pth, fwr) -> {
                if (fwr.remove(path, consumer)) {
                    toRemove.add(pth);
                }
            });
            toRemove.forEach(registrations::remove);
            return isEmpty();
        }

        synchronized boolean isEmpty() {
            if (registrations.isEmpty()) {
                return true;
            }
            Set<Path> toRemove = new HashSet<>();
            registrations.forEach((pth, fwr) -> {
                if (fwr.isEmpty()) {
                    toRemove.add(pth);
                }
            });
            toRemove.forEach(registrations::remove);
            boolean result = registrations.isEmpty();
            if (result) {
                close();
            }
            return result;
        }

        synchronized void close() {
            if (svc != null) {
                try {
                    svc.close();
                    svc = null;
                } catch (IOException ex) {
                    Logger.getLogger(WatchManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                registrations.clear();
            }
        }

        synchronized void add(Path folder, Path target, Set<WatchEvent.Kind<?>> kinds, BiConsumer<Path, WatchEvent.Kind<?>> c) throws IOException {
            FolderWatchRegistration reg = registrations.computeIfAbsent(folder, FolderWatchRegistration::new);
            WatchService ws = watchService();
            reg.add(target, ws, c, kinds);
        }

        synchronized WatchService watchService() throws IOException {
            if (svc != null) {
                return svc;
            }
            svc = fs.newWatchService();
            return svc;
        }

        int pollLoop(long ms, long maxTimeMillis, Bool anyTimedOut) throws InterruptedException {
//            System.out.println("  fs poll loop start " + fs);
            WatchKey key;
            Int processed = Int.create();
            long start = System.currentTimeMillis();
            do {
                WatchService ws;
                synchronized (this) {
                    ws = svc;
                }
                if (ws == null) {
                    break;
                }
                if (System.currentTimeMillis() - start > maxTimeMillis) {
                    anyTimedOut.set();
                    break;
                }
                key = ms == 0 ? ws.poll() : ws.poll(ms, TimeUnit.MILLISECONDS);
                if (key != null) {
                    WatchKey wk = key;
                    try {
                        List<WatchEvent<?>> evts = key.pollEvents();
                        evts.forEach(evt -> {
                            if (!(evt.context() instanceof Path)) {
                                return;
                            }
                            registrations.forEach((path, fwr) -> {
                                int count = fwr.eachConsumer(wk, evt, consumer -> {
                                    Path ctx = (Path) evt.context();
                                    consumer.accept(path.resolve(ctx), evt.kind());
                                });
                                processed.increment(count);
                            });
                        });
                    } finally {
                        wk.reset();
                    }
                }
            } while (key != null);
            if (processed.getAsInt() > 0) {
                StringBuilder sb = new StringBuilder();
                for (Path dr : fs.getRootDirectories()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(dr);
                }
            }
            return processed.getAsInt();
        }
    }

    private static class FolderWatchRegistration {

        private final Path folder;
        private final Set<OneWatch> watches = new HashSet<>();
        private final Map<WatchEvent.Kind<?>, WatchKey> keyForKind = new IdentityHashMap<>();

        public FolderWatchRegistration(Path folder) {
            this.folder = folder;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Map<WatchEvent.Kind<?>, WatchKey> kc;
            Set<OneWatch> ows;
            synchronized (this) {
                kc = new HashMap<>(keyForKind);
                ows = new HashSet<>(watches);
            }
            for (Map.Entry<WatchEvent.Kind<?>, WatchKey> e : kc.entrySet()) {
                sb.append("\n\t\t").append(e.getKey()).append(' ').append(e.getValue());
            }
            for (OneWatch w : ows) {
                sb.append("\n\t\t\t").append(w);
            }
            return sb.toString();
        }

        void ensureClosed() {
            for (Map.Entry<WatchEvent.Kind<?>, WatchKey> e : keyForKind.entrySet()) {
                e.getValue().cancel();
            }
            keyForKind.clear();
        }

        boolean remove(Path path, BiConsumer<Path, WatchEvent.Kind<?>> consumer) {
            Set<OneWatch> toRemove = new HashSet<>();
            for (OneWatch ow : watches) {
                if (ow.is(path, consumer)) {
                    toRemove.add(ow);
                }
            }
            if (!toRemove.isEmpty()) {
                cleanup(toRemove);
            }
            return isEmpty();
        }

        boolean isEmpty() {
            if (watches.isEmpty()) {
                ensureClosed();
                return true;
            }
            Set<OneWatch> toRemove = gc();
            cleanup(toRemove);
            boolean result = watches.isEmpty();
            if (result) {
                ensureClosed();
            }
            return result;
        }

        void cleanup(Set<OneWatch> toRemove) {
            // This is fairly fussy, as we may be listening for more than
            // one kind with a single watch key, and we do not want to kill
            // watch keys unless there really is nothing that will ever be
            // interested in anything they have to say
            if (!toRemove.isEmpty()) {
                watches.removeAll(toRemove);
                Set<WatchKey> possiblyRemoveKeys = new HashSet<>();
                toRemove.forEach(ow -> {
                    ow.kinds.stream().map(k -> keyForKind.get(k))
                            .filter(key -> (key != null))
                            .forEach(key -> {
                                possiblyRemoveKeys.add(key);
                            });
                });
                Set<WatchEvent.Kind<?>> remaining = kinds();
                remaining.stream()
                        .map(rem -> keyForKind.get(rem))
                        .filter(wk -> (wk != null))
                        .forEachOrdered(wk -> {
                            possiblyRemoveKeys.remove(wk);
                        });
                possiblyRemoveKeys.forEach(remove -> {
                    remove.cancel();
                });
            }
        }

        @SuppressWarnings("unchecked")
        int eachConsumer(WatchKey key, WatchEvent<?> evt, Consumer<BiConsumer<Path, WatchEvent.Kind<?>>> cc) {
            if (!(evt.context() instanceof Path)) {
                return 0;
            }
            WatchEvent<Path> e = (WatchEvent<Path>) evt;

            WatchKey k = keyForKind.get(evt.kind());
            int result = 0;
            if (k != null && k.equals(key)) {
                for (OneWatch w : watches) {
                    BiConsumer<Path, WatchEvent.Kind<?>> c = w.match(folder, e);
                    if (c != null) {
                        result++;
                        cc.accept(c);
                    }
                }
            }
            return result;
        }

        synchronized void add(Path target, WatchService svf, BiConsumer<Path, WatchEvent.Kind<?>> c, Set<WatchEvent.Kind<?>> kinds) throws IOException {
            Set<OneWatch> deadButNotRemoved = gc();
            Set<WatchEvent.Kind<?>> currentKinds = keyForKind.keySet();
            Set<WatchEvent.Kind<?>> toAdd = new HashSet<>(kinds);
            toAdd.removeAll(currentKinds);
            OneWatch nue = new OneWatch(target, kinds, c);
            watches.add(nue);
            watches.removeAll(deadButNotRemoved);

            if (!toAdd.isEmpty()) {
                WatchKey wk = folder.register(svf, kinds.toArray(new WatchEvent.Kind<?>[toAdd.size()]));
                for (WatchEvent.Kind<?> k : toAdd) {
                    keyForKind.put(k, wk);
                }
            }
            Set<WatchEvent.Kind<?>> noLongerNeeded = new HashSet<>(keyForKind.keySet());
            noLongerNeeded.removeAll(kinds);
            for (WatchEvent.Kind<?> k : noLongerNeeded) {
                WatchKey key = keyForKind.remove(k);
                if (key != null) {
                    key.cancel();
                }
            }
        }

        private WatchEvent.Kind<?>[] kindsArray() {
            Set<WatchEvent.Kind<?>> kinds = kinds();
            return kinds.toArray(new WatchEvent.Kind<?>[kinds.size()]);
        }

        private Set<WatchEvent.Kind<?>> kinds() {
            Set<WatchEvent.Kind<?>> all = new HashSet<>();
            for (OneWatch ow : watches) {
                all.addAll(ow.kinds);
            }
            return all;
        }

        Set<OneWatch> gc() {
            Set<OneWatch> toRemove = new HashSet<>();
            for (OneWatch ow : watches) {
                if (ow.isGone()) {
                    toRemove.add(ow);
                }
            }
            return toRemove;
        }

        FolderWatchRegistration add(Path target, BiConsumer<Path, WatchEvent.Kind<?>> consumer, WatchEvent.Kind<?> kind, WatchEvent.Kind<?>... more) {
            Set<WatchEvent.Kind<?>> kinds = new HashSet<>();
            kinds.add(kind);
            kinds.addAll(Arrays.asList(more));
            watches.add(new OneWatch(target, kinds, consumer));
            return this;
        }

        static class OneWatch {

            private final Set<WatchEvent.Kind<?>> kinds;
            private final Reference<BiConsumer<Path, WatchEvent.Kind<?>>> consumer;
            private final Path target;
            private boolean isFile;

            public OneWatch(Path target, Set<WatchEvent.Kind<?>> kinds, BiConsumer<Path, WatchEvent.Kind<?>> consumer) {
                this.kinds = kinds;
                this.consumer = new WeakReference<>(consumer);
                this.target = target;
                isFile = !Files.isDirectory(target);
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append(target);
                if (isFile) {
                    sb.append(" (file)");
                }
                sb.append(" alive? ").append(consumer.get() != null);
                for (WatchEvent.Kind<?> k : kinds) {
                    sb.append(' ').append(k.name());
                }
                sb.append(' ').append(consumer.get());
                return sb.toString();
            }

            boolean is(Path path, BiConsumer<Path, WatchEvent.Kind<?>> consumer) {
                if (!target.equals(path)) {
                    return false;
                }
                if (consumer != null) {
                    BiConsumer<Path, WatchEvent.Kind<?>> ours = this.consumer.get();
                    return consumer.equals(ours);
                }
                return true;
            }

            BiConsumer<Path, WatchEvent.Kind<?>> match(Path parent, WatchEvent<Path> event) {
                if (!kinds.contains(event.kind())) {
                    return null;
                }
                Path et = event.context();
                if (isFile) {
                    Path real = parent.resolve(et);
                    if (target.equals(real)) {
                        return consumer.get();
                    }
                } else {
                    return consumer.get();
                }
                return null;
            }

            boolean isGone() {
                return consumer.get() == null;
            }
        }
    }
    /*
    private static final class C implements BiConsumer<Path, WatchEvent.Kind<?>> {

        private final String name;

        public C(String name) {
            this.name = name;
        }

        @Override
        public void accept(Path path, WatchEvent.Kind<?> kind) {
            System.out.println("\n" + name + " " + kind + " " + " " + path);
        }

        public String toString() {
            return "C(" + name + ")";
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ScheduledExecutorService svc = Executors.newScheduledThreadPool(1);
        WatchManager mgr = new WatchManager(svc, 20, 120, 500);
        BiConsumer<Path, WatchEvent.Kind<?>> c = new C("FILE");
        BiConsumer<Path, WatchEvent.Kind<?>> c1 = new C("ANY");
        mgr.watch(Paths.get("/tmp/foo/bar.txt"), c, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        mgr.watch(Paths.get("/tmp/foo"), c1, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

        BiConsumer<Path, WatchEvent.Kind<?>> cRecur = new C("RECURSIVE");

        RecursiveWatcher rw = mgr.recursiveWatch(Paths.get("/tmp/foo"), 4,
                cRecur, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

        System.out.println("Service:\n" + mgr);

        Thread.sleep(12000);
        System.out.println("Now letting one go");
        c = null;
        for (int i = 0; i < 50; i++) {
            System.gc();
            System.runFinalization();
        }
        mgr.isEmpty();
        Thread.sleep(1000);
        for (int i = 0; i < 50; i++) {
            System.gc();
            System.runFinalization();
        }
        mgr.isEmpty();
        for (int i = 0; i < 50; i++) {
            System.gc();
            System.runFinalization();
        }
        System.out.println("File watcher should be gone. Empty should be false. " + mgr.isEmpty());

        System.out.println("Service:\n" + mgr);

        Thread.sleep(10000);
        mgr.unwatch(Paths.get("/tmp/foo"), c1);
        rw.detach();
        System.out.println("No longer watching");

        Thread.sleep(12000);
        System.out.println("Empty? " + mgr.isEmpty());
        System.out.println("Service:\n" + mgr);

        System.out.println("\nWill start watching again in 5 secs");
        Thread.sleep(5000);

        mgr.watch(Paths.get("/tmp/foo"), c1, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        rw.attach();
        System.out.println("Now watching again");

        System.out.println("Service:\n" + mgr);
        Thread.sleep(10000);

        System.out.println("Recursively listening agian");
        rw.attach();

        Thread.sleep(10000);

        System.out.println("Shutting down");
        mgr.shutdown();
        System.out.println("Service:\n" + mgr);

        Thread.currentThread().join();
    }
     */
}
