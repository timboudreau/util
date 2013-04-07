/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
package com.mastfrog.util.thread;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Tim Boudreau
 */
public class LoggingExecutorService implements ExecutorService {

    private final ExecutorService svc;
    private final String name;

    LoggingExecutorService(ExecutorService svc) {
        this(svc.toString(), svc);
    }

    LoggingExecutorService(String name, ExecutorService svc) {
        this.svc = svc;
        this.name = name;
    }

    public static ExecutorService wrap(String name, ExecutorService svc) {
        if (svc instanceof LoggingExecutorService) {
            return svc;
        }
        return new LoggingExecutorService(name, svc);
    }

    public static ExecutorService wrap(ExecutorService svc) {
        if (svc instanceof LoggingExecutorService) {
            return svc;
        }
        return new LoggingExecutorService(svc);
    }
    
    @Override
    public String toString() {
        return "Wrapper for " + name + " (" + svc + ")";
    }

    @Override
    public void shutdown() {
        svc.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return svc.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return svc.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return svc.isTerminated();
    }

    @Override
    public boolean awaitTermination(long l, TimeUnit tu) throws InterruptedException {
        return svc.awaitTermination(l, tu);
    }

    @Override
    public <T> Future<T> submit(Callable<T> clbl) {
        System.out.println("Submit " + clbl);
        return svc.submit(clbl);
    }

    @Override
    public <T> Future<T> submit(Runnable r, T t) {
        System.out.println("Submit " + r);
        return svc.submit(r, t);
    }

    @Override
    public Future<?> submit(Runnable r) {
        System.out.println("Submit " + r);
        return svc.submit(r);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> clctn) throws InterruptedException {
        return svc.invokeAll(clctn);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> clctn, long l, TimeUnit tu) throws InterruptedException {
        return svc.invokeAll(clctn, l, tu);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> clctn) throws InterruptedException, ExecutionException {
        return svc.invokeAny(clctn);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> clctn, long l, TimeUnit tu) throws InterruptedException, ExecutionException, TimeoutException {
        return svc.invokeAny(clctn, l, tu);
    }

    @Override
    public void execute(Runnable r) {
        System.out.println("Execute " + r);
        try {
            svc.execute(r);
        } finally {
            System.out.println(" DONE: " + r);
        }
    }
}
