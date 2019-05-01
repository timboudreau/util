Mastfrog Utilities
==================

Every library has its collection of pet utilities, and this is no exception.  
Useful stuff here:

 * `bits` - API-compatible BitSet-like classes which can wrap Java BitSets (or not), which work as a drop-in replacement and provide
   * A read-only interface to a bit set, `Bits` so you can expose one in your API without opening it to alteration
   * Synchronized wrappers for mutable `MutableBits`
   * Long-indexed implementations which can contain more than `Integer.MAX_VALUE` bits, with pluggable backing storage using
     * Java long arrays
     * Off-heap memory allocated via `sun.misc.Unsafe`
     * Memory-mapped NIO files which can persist and be reloaded
 * `range` - A set of interfaces and implementations for dealing conveniently with ranges (things with a numeric start and size) -
a common thing to deal in when writing memory managers, code completion, or dealing with time-series data. If you've ever 
coded this stuff, you know it's death-by-off-by-one errors.  I never wanted to do so again.  Features:
   * Base `Range` class which implements most features
   * Primitive `int` and `long` based specializations of those with type specific performance optimizations
   * Support of _coalescing_ ranges - for example, in the case of code completion, say you have a range 10:20 that should be 
italic, and a range 15:25 that should be bold.  To actually display this, you need to coalesce that to 10:15-italic, 
15:20-bold+italic, 20-25:bold.  Support for this is built-in - you just provide a `Coalescer` which generates the combined bold+italic.
   * Range sorting, comparisons and relationships
 * `graph` - Builds on `bits` to create high-performance, low-memory-footprint graphs from arrays of bit sets, with support
for graph algorithms and built in implementation of eigenvector centrality.  Combined with the support for large bit sets,
this can be the basis for high-performance in-memory graph processing.
 * `function` - `java.util.function` is great until a lambda calls something that throws a checked exception, at which point
things get ugly.  This package provides the missing throwing equivalents to what the JDK provides, plus some useful logical
extensions such as `ByteSupplier`.
 * `util-collections` - Collection-and collection-related classes for performance and code-bloat reduction, mostly accessed via
static methods on `CollectionUtilities`.  For example:
   * `CollectionUtils.supplierMap(Supplier&lt;T&gt;)` - build caches simply with a supplier that provides missing values 
- e.g. `List&lt;String,List&lt;String&gt;&gt; l = CollectionUtils.supplierMap(ArrayList::new); l.get("foo").add("bar")`
   * `setOf(a,b,c)` - quickly create a `Set` from some objects (implementation will pick the best Set implementation based on type)
   * Map builders - e.g. `Map&lt;String,String&gt; m = map("foo").to("bar").map("baz").finallyTo("quux");`
   * High-performance array/binary-search based int-keyed maps
   * Creating lists, sets and iterables from multiple others by wrapping, not copying
   * Utilities for dealing with arrays, splitting and concatenating
   * Map-like extractions for type-safe, multi-type maps
   * Utilities to implement binary search over anything that can be resolved to a Java long
 * `util-fileformat` - Minimal tools for writing valid JSON (for when a library like Jackson is too much), and for
writing `.properties` files identically to the way the JDK does, minus the unavoidable timestamp comment that results
in non-repeatable builds when used in annotation processors
 * `util-time` - takes the pain out of dealing with JDK 8's `java.time` package for common tasks, and provides a
replacement for Joda Time's `Interval`, which the JDK does not have
 * `util-net` - An generic incremental backoff implementation; parsers for IPv4 and IPv6 addresses which will not
ever, ever make a network connection to answer `equals()` - just bland little objects, as they should be, with no magic;
and a utility useful in parallel tests for finding an available server port.
 * `util-preconditions` - Similar to other preconditions (check that this or that is non-null, non-negative, etc.) except
that by defining its own exceptions, you can differentiate programmer error from bad input more easily.
 * `util-streams` - the usual stream-reading utilities, plus support for tailing a file as a kind of stream
 * `util-strings` - various string-based utilities, plus support for tab-aligned pretty printing, and eight-bit strings
with deduplication and disposable intern pools, for working with huge amounts of data in limited memory
 * `util-thread` - Concurrency utilities:
    * `AtomicLinkedQueue` - a thread-safe tail-linked data structure using atomic operations
    * `BufferPool` - keep a thread-safe pool of direct ByteBuffers for reuse and borrow and return them
    * `AtomicRoundRobin` - loop through a sequence of numbers atomically
    * `ResettableCountDownLatch` - a reusable `CountDownLatch`
    * `AtomicMaximum` - atomically track the maximum value of some number, with support for measuring thread contention
    * Various `ThreadLocal` extensions, such as using `AutoCloseable` to set and unset, using a `Supplier` to provide
a default value, and more
    * `EnhCompletableFuture` - an extension to the JDK's `CompletableFuture` which makes combining and chaining
and transformations much similar, taking advantage of the throwing lambdas from `function`, and including support for
named futures for logging purposes

The original, monolithic `utils` library is present but its contents are deprecated and delegates to these libraries.

Builds and a Maven repository containing this project can be <a href="https://timboudreau.com/builds/">found on timboudreau.com</a>.
