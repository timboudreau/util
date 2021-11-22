Mastfrog Utilities
==================

Miscellaneous general-purpose utilities useful in many types of project.

Everything here is available from Maven Central under `com.mastfrog:$PROJECTNAME`.

What's here
-----------

 * `bits` - API-compatible BitSet-like classes which can wrap Java BitSets (or not), which work as a drop-in replacement and provide
   * A read-only interface to a bit set, `Bits` so you can expose one in your API without opening it to alteration
   * Synchronized wrappers for mutable `MutableBits`
   * Long-indexed implementations which can contain more than `Integer.MAX_VALUE` bits, with pluggable backing storage using
     * Java long arrays
     * Off-heap memory allocated via `sun.misc.Unsafe`
     * Memory-mapped NIO files which can persist and be reloaded
 * `concurrent` - Misc concurrency primitives and concurrent statistics collectors, combinable multi-slot locks
 * `range` - A set of interfaces and implementations for dealing conveniently with ranges (things with a numeric start and size) -
a common thing to deal in when writing memory managers, code completion, or dealing with time-series data. If you've ever 
coded this stuff, you know it's death-by-off-by-one errors.  I never wanted to do so again.  Features:
   * Base `Range` class which implements most features
   * Primitive `int` and `long` based specializations of those with type specific performance optimizations
   * Support for sorting _and coalescing_ ranges - for example, in the case of code completion, say you have a range 10:20 that should be 
italic, and a range 15:25 that should be bold.  To actually display this, you need to coalesce that to 10:15-italic, 
15:20-bold+italic, 20-25:bold.  Support for this is built-in - you just provide a `Coalescer` which generates the combined bold+italic.
   * Range sorting, comparisons and relationships
 * `graph` - High-performance, low-memory-footprint graphs (directed and undirected, cyclic and acyclic) based on arrays of `BitSets`
   * Wrapper object graphs that create graphs of objects based on integer-based lookup of objects corresponding to 
     the graph node indices, which work with any type that is `Comparable` or that a `Comparator` can be provided for
   * Built-in implementations of the _page rank_ and _eigenvector centrality_ algorithms, and the ability to plug in your own
   * Building on the `bits` library, supports graphs larger than `Integer.MAX_VALUE`
 * `function` - `java.util.function` is great until a lambda calls something that throws a checked exception, at which point
things get ugly.  This package provides the missing throwing equivalents to what the JDK provides, plus some useful logical
extensions such as `ByteSupplier`.  Specifically
   * `Exception-` and `IOException-`throwing variants of the standard functional interfaces
   * 3- through 8-argument variants of the consumer, function and predicate patterns in throwing and non-throwing variants
   * `float`- and `byte-` based equivalents of consumer, function and predicate patterns
   * "holder" types for state which must cross lambda boundaries and cannot otherwise be implemented simply, because variables declared above and used in lambdas are final:
      * Holders for objects, booleans, doubles, floats, including atomic variants
 * `search` - generic binary-search that can be applied to any sorted backing-store, such as byte buffers over fixed-length record files
 * `sort` - generic sorting of any backing store that contains elements that can be compared, for which a Swapper can be implemented;
   sorting pairs of arrays by the contents of one of them
 * `util-collections` - Collection-and collection-related classes for performance and code-bloat reduction, mostly accessed via
static methods on `CollectionUtils`.  For example:
   * `CollectionUtils.supplierMap(Supplier&lt;T&gt;)` - build caches simply with a supplier that provides missing values, e.g. `List&lt;String,List&lt;String&gt;&gt; l = CollectionUtils.supplierMap(ArrayList::new); l.get("foo").add("bar")`
   * `setOf(a,b,c)` - quickly create a `Set` from some objects (implementation will pick the best Set implementation based on type)
   * Map builders - e.g. `Map&lt;String,String&gt; m = map("foo").to("bar").map("baz").finallyTo("quux");`
   * High-performance array/binary-search based primitive int- and long-keyed maps and sets
   * Creating lists, sets and iterables from multiple others by wrapping, not copying
   * Utilities for dealing with arrays, splitting and concatenating
   * Map-like extractions for type-safe, multi-type maps
   * Timed-eviction caches
 * `util-fileformat` - Minimal tools for writing valid JSON (for when a library like Jackson is too much), and for
writing `.properties` files identically to the way the JDK does, minus the unavoidable timestamp comment that results
in non-repeatable builds when used in annotation processors
 * `util-time` - takes the pain out of dealing with JDK 8's `java.time` package for common tasks
    * Parsing and generating HTTP and ISO formats
    * Flexible formatters and parsers for `Duration` instances
    * API-compatible replacement for Joda Time's `Interval`, `ReadableInterval` and `MutableInterval` over `java.time`,
      which were very useful for time-series data, but, despite `java.time` having the same author, absent from the
      JDK's time classes
    * `TimeUtil` which makes conversion between unix timestamps and `Instant` / `ZonedDateTime` / `LocalDateTime` 
       as straightforward as it was in Joda Time (`java.time` works in seconds + nanoseconds as its base units, and wants
       you to as well - most legacy code uses milliseconds, making it fussy to adapt)
 * `util-net` - A few useful networking utilities, including
   * Generic incremental backoff implementations with configurable policy and backoff algorithms
   * Parsers for IPv4 and IPv6 addresses which will not ever, ever make a network connection 
     to answer `equals()` - just bland little objects, as they should be, with no magic;
   * `PortFinder` - for test framworks, reliably find available network ports to run tests that start
     servers concurrently without port-collisions causing unexpected failures
 * `util-preconditions` - Similar to other preconditions (check that this or that is non-null, non-negative, etc.) except
that by defining its own exceptions, you can differentiate programmer error from caller-error more easily.
 * `util-streams` - Stream- and IO-related utilities, including
    * Streams and related utilities for tailing files - `Tail`, `ContinuousLineStream` and `ContinuousStringStream`
    * NIO channel implementations over in-memory data - the equivalent of `ByteArray*Stream` for NIO, so NIO can fully replace OIO in libraries that are not always working with disk files
    * Streams that generate a `MessageDigest` hash as bytes are pulled through them
    * `ThreadMappedStdIO` - replace `System.out` and `System.err` on a per-thread basis, for collecting (or discarding) output from
      libraries that insist on using these, or capturing the output from external processes without the complexity of pipes -
      just provide a `PrintStream` to write to and pass a lambda, and all output within its closure will go to the passed stream
    * `UnixPath` - platform independently consistent version of `java.nio.Path`, useful for writing in-memory filesystems
    * `Streams` - null streams and writers, tee-streams, streams over NIO channels and `ByteBuffer`s, and the usual string-reading utilities
 * `util-strings` - various string-based utilities, plus support for tab-aligned pretty printing, and eight-bit strings
with deduplication and disposable intern pools, for working with huge amounts of data in limited memory
 * `util-thread` - Concurrency utilities:
    * `AtomicLinkedQueue` - a thread-safe tail-linked list-like data structure using atomic operations
    * `BufferPool` - keep a thread-safe pool of direct ByteBuffers for reuse and borrow and return them
    * `AtomicRoundRobin` - loop through a sequence of numbers atomically
    * `ResettableCountDownLatch` - a reusable `CountDownLatch`
    * `AtomicMaximum` - atomically track the maximum value of some number, with support for measuring thread contention
    * Various `ThreadLocal` extensions, such as using `AutoCloseable` to set and unset, using a `Supplier` to provide
a default value, and more
    * `EnhCompletableFuture` - an extension to the JDK's `CompletableFuture` which makes combining and chaining
and transformations much similar, taking advantage of the throwing lambdas from `function`, and including support for
named futures for logging purposes
 * `subscription` - the ultimate variant on the _listener pattern_ - builder-based utilities to construct one-to-many collections of objects interested in
   "events" on some object type, with complete flexibility as to how the collections of objects remain referenced, the
   way "listeners" are referenced, the dispatch mechanism (immediate, asynchronous, asynchronous-coalesced) for those events,
   with no assumptions made about what types of objects those things are - you provide the actual dispatch mechanism, this
   library provides the storage and the invocation
 * `abstractions` - Core abstractions which pervade Java code and are useful to decompose into their constituent parts - often when
   implementing an algorithm such as sorting or binary search, tying that implementation to a particular collection type such as `List`
   limits its usefulness or forces callers to copy their data into a list, when only a subset of the behaviour of a list is required;
   using these abstractions instead eliminates those limitations:
   * `Named` - things which have a name
   * `Wrapper` - objects which wrap or encapsulate another object and want to expose type-based lookup of the original object or anything it wraps recusively
   * `Stringifier` - types which convert to strings
   * `Copyable` - objects which can have independent copies of themselves made
   * Decomposition of the concept of a _list_:
     * `IntSized`, `LongSized` - things which have a size
     * `Indexed` - things that can be iterated in a defined order
     * `IntAddressable`, `LongAddressable` - containers for elements that can be looked up by a numeric index
     * `IndexedResolvable`, `LongResolvable` - containers that can be queried for the index of an element
 * `converters` - Flexibly convert objects of multiple types into each other - you register converters with a `Converters` instance,
    and it uses the `graph` library to find the shortest workable conversion path - so if you registered converter for `Path` to `CharSequence`
    and a converter for `File` to `Path`, and a caller requests that a `File` be converted to a `CharSequence`, it can resolve the
    intermediate steps and perform the operation, using the shortest path within the graph between the two types to minimize
    steps

Builds and a Maven repository containing this project can be <a href="https://timboudreau.com/builds/">found on timboudreau.com</a>.
