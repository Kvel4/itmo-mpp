import java.util.concurrent.atomic.AtomicReference

class Solution(val env: Environment) : Lock<Solution.Node> {
    private val tail = AtomicReference<Node?>(null)

    override fun lock(): Node {
        val my = Node()
        val prev = tail.getAndSet(my)
        if (prev != null) {
            prev.next.value = my
            while (my.blocked.value) { env.park() }
        }
        return my // вернули узел
    }

    override fun unlock(node: Node) {
        if (node.next.value == null) {
            if (tail.compareAndSet(node, null)) return
            while (node.next.value == null) {
                // wait
            }
        }
        node.next.value!!.blocked.value = false
        env.unpark(node.next.value!!.thread)
    }

    class Node {
        val thread: Thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val blocked = AtomicReference(true)
        val next = AtomicReference<Node?>(null)
    }
}