import io.netty.util.concurrent.Future
import io.netty.util.concurrent.FutureListener
import org.redisson.client.RedisClient
import org.redisson.client.RedisConnection
import org.redisson.client.protocol.RedisCommands
import java.util.concurrent.CountDownLatch

fun main(args: Array<String>) {
    var c = RedisClient("localhost", 6379)
    var f = c.connectAsync()
    var l = CountDownLatch(1)
    f.addListener(object : FutureListener<RedisConnection> {
        @Throws(Exception::class)
        override fun operationComplete(future: Future<RedisConnection>) {
            val conn = future.get()
            println(conn.sync(RedisCommands.PING))
            l.countDown()
        }
    })
    l.await()
    c.shutdown()
    println("exiting...")
    Runtime.getRuntime().halt(0)
}
