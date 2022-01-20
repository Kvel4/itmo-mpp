import kotlinx.atomicfu.*

class FAAQueue<T> {
    private val head: AtomicRef<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicRef<Segment> // Tail pointer, similarly to the Michael-Scott queue

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(x: T) {
        while (true) {
            val tailCopy = tail.value
            val enqIdx = tailCopy.enqIdx.getAndIncrement()

            if (enqIdx >= SEGMENT_SIZE) {
                if (tailCopy.next.value != null) {
                    tail.compareAndSet(tailCopy, tailCopy.next.value!!)
                    continue
                }
                val newTail = Segment(x)
                if (tailCopy.next.compareAndSet(null, newTail)) {
                    tail.compareAndSet(tailCopy, newTail)
                    return
                }
                continue
            }
            if (tailCopy.elements[enqIdx].compareAndSet(null, x)) return
        }
    }

    /**
     * Retrieves the first element from the queue
     * and returns it; returns `null` if the queue
     * is empty.
     */
    fun dequeue(): T? {
        while (true) {
            val headCopy = head.value
            val deqIdx = headCopy.deqIdx.getAndIncrement()

            if (deqIdx >= SEGMENT_SIZE) {
                val headNext = headCopy.next.value ?: return null

                head.compareAndSet(headCopy, headNext)
                continue
            }

            val res = headCopy.elements[deqIdx].getAndSet(DONE) ?: continue
            return res as T?
        }
    }

    /**
     * Returns `true` if this queue is empty;
     * `false` otherwise.
     */
    val isEmpty: Boolean get() {
        while (true) {
            val headCopy = head.value

            if (headCopy.isEmpty) {
                if (headCopy.next.value == null) return true

                head.compareAndSet(headCopy, head.value.next.value!!)
                continue
            } else {
                return false
            }
        }
    }
}

private class Segment {
    val next = atomic<Segment?>(null)
    val enqIdx = atomic(0) // index for the next enqueue operation
    val deqIdx = atomic(0) // index for the next dequeue operation
    val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)

    constructor() // for the first segment creation

    constructor(x: Any?) { // each next new segment should be constructed with an element
        enqIdx.value = 1
        elements[0].value = x
    }

    val isEmpty: Boolean get() = deqIdx.value >= enqIdx.value || deqIdx.value >= SEGMENT_SIZE
}

private val DONE = Any() // Marker for the "DONE" slot state; to avoid memory leaks
const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS

