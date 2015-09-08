package com.irrlab.kotlin.web

import spark.Spark.*

import com.google.gson.Gson
import spark.*
import java.util.*

class JsonTransformer : ResponseTransformer {
    private val gson = Gson()
    override fun render(model: Any): String {
        return gson.toJson(model)
    }
}

class Router {
    public fun main(args: Array<String>) {
        get("/test", object : Route {
            override fun handle(request: Request, response: Response): Any {
                val map = hashMapOf(200 to "Test OK!");
                response.type("application/json")
                return map
            }
        }, JsonTransformer())
    }
}

fun main(args : Array<String>) {
    Router().main(args)
}
