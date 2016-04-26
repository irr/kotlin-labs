package com.irrlab.kotlin.nsq

import com.github.brainlag.nsq.NSQConsumer
import com.github.brainlag.nsq.NSQProducer
import com.github.brainlag.nsq.lookup.DefaultNSQLookup
import com.google.gson.Gson
import spark.Request
import spark.Response
import spark.ResponseTransformer
import spark.Route
import spark.Spark.get
import kotlin.concurrent.thread


class JsonTransformer : ResponseTransformer {
    private val gson = Gson()
    override fun render(model: Any): String {
        return gson.toJson(model)
    }
}

class Router() {
    fun main(args: Array<String>) {
        get("/test", object : Route {
            override fun handle(request: Request, response: Response): Any {
                response.type("application/json")
                val map = hashMapOf(200 to "Test OK!");
                return map
            }
        }, JsonTransformer())
    }
}

/*
curl -i -H "Content-Type:application/json" -X POST -d "{'id':'200','token':'ab2cdef3434bfc3ssdhhf'}" 'http://127.0.0.1:4151/put?topic=topic'
 */

fun main(args : Array<String>) {
    thread() {
        val lookup = DefaultNSQLookup()
        lookup.addLookupAddress("127.0.0.1", 4161)
        val consumer = NSQConsumer(lookup, "topic", "channel", { message ->
            println("received: " + String(message.message))
            message.finished()
        }).setLookupPeriod(5000)

        consumer.start()
    }

    thread {
        val configs = hashMapOf(
                Pair(0, "127.0.0.1" to 4150),
                Pair(1, "127.0.0.1" to 5150))
        val producers = listOf(
                NSQProducer().addAddress(configs[0]!!.first, configs[0]!!.second).start(),
                NSQProducer().addAddress(configs[1]!!.first, configs[1]!!.second).start()
        )
        val gson = Gson()
        for (n in 1..10) {
            val map = hashMapOf(n to "message $n sent to ${configs[n % 2]}")
            producers[n % 2].produce("topic", gson.toJson(map).toByteArray());
        }
    }
    Router().main(args)
}

