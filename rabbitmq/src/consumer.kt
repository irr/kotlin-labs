import com.rabbitmq.client.*
import java.nio.charset.Charset

fun main(args: Array<String>) {

    val uri = "amqp://guest:guest@localhost:5672/%2F?heartbeat_interval=1000"
    val factory = ConnectionFactory()
    factory.setUri(uri)

    val conn = factory.newConnection()
    val channel = conn.createChannel()

    val autoAck = false
    channel.basicConsume("test", autoAck,
            object : DefaultConsumer(channel) {
                override fun handleDelivery(consumerTag: String?,
                                            envelope: Envelope?,
                                            properties: AMQP.BasicProperties?,
                                            body: ByteArray?) {
                    val deliveryTag = envelope!!.deliveryTag
                    val message = body!!.toString(Charset.defaultCharset())
                    println("deliveryTag=$deliveryTag and body=$message")
                    channel.basicAck(deliveryTag, false);
                }
            })
}