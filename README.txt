See http://jakarta.apache.org/commons/pool/ for additional and 
up-to-date information on Commons Pool.

Pool 1.3 is largely a bug fix only release. For a complete set of release
notes see: http://jakarta.apache.org/commons/pool/release-notes-1.3.html

Notable Bugfixes since 1.2:

* GenericObjectPools are now a FIFOs. Previously they was documented as such
  but actually implemented as a LIFOs.

* Synchronizations improvements across all implementations.

Notable Additions since 1.2:

* GenericObjectPool introduced a SoftMinEvictableIdleTimeMillis property
  which can be used to evict idle objects so long as their eviction
  would maintain the requested minIdle count.

* PoolUtils contains a number of utility methods to decorate pools.

* The one dependency on Commons Collections has been removed (okay,
  technically a deletion) and the only requirement is Java 1.3 or above.

Miscellaneous issues:

Some of the unit tests may fail spuriously because they are trying to test
behavior that depends on the thread scheduler or the garbage collector. If
you get a test failure that has "evictor" or "thread" in the test name,
please run this test a few times before reporting a bug report for it. If
you know how to make one of these unit tests better, please submit a patch.


The Apache Jakarta Commons Team, March 2006