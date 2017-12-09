import ch.hsr.geohash.GeoHash

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

import java.io.InputStream
import java.io.OutputStream
import java.util.*


enum class DevProfile {
    LOCAL, REMOTE
}


class Constants {
    companion object {
        val DEFAULT = DevProfile.LOCAL
        val LATITUDE = 38.7233682
        val LONGITUDE = -9.1393366
    }
}


data class UserSimple(val name: String,
                      val email: String,
                      val age: Int? = null)


data class Place(@SerializedName("place") val place: String,
                 @SerializedName("latitude") val latitude: Double,
                 @SerializedName("longitude") val longitude: Double,
                 @SerializedName("dummy") var dummy: Int? = null)


data class Hashes(val geohash: String,
                  val neighbors: List<String>)


fun geohasher(latitude: Double, longitude: Double, precision: Int = 9): Hashes {
    val hash = GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude, precision)
    val neighbors = GeoHash.fromGeohashString(hash).adjacent.map { i -> i.toBase32() }
    return Hashes(hash, neighbors.plusElement(hash))
}


fun getDynamoDBClient(profile: DevProfile = Constants.DEFAULT, name: String = "personal") : AmazonDynamoDB {
    val credentialsProvider = ProfileCredentialsProvider(name)
    if (profile == DevProfile.LOCAL) {
        val dynamodb = AmazonDynamoDBClient(credentialsProvider)
        dynamodb.setEndpoint("http://localhost:8000")
        return dynamodb
    }
    return AmazonDynamoDBClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion("us-east-1")
            .build()
}


object DynamoDBTest {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val hashes = (5..9).map { precision -> geohasher(Constants.LATITUDE, Constants.LONGITUDE, precision) }
            println(hashes)

            val gson = Gson()
            val user = UserSimple("ivan rocha", "ivan.ribeiro@gmail.com")
            println(gson.toJson(user))

            val userJSON = "{'age':26,'email':'norman@futurestud.io','isDeveloper':true,'name':'Norman'}"

            val userObj = gson.fromJson(userJSON, UserSimple::class.java)
            println(userObj)

            val placeJSON = "{'place': 'lisbon','latitude': ${Constants.LATITUDE}, 'longitude': ${Constants.LONGITUDE}}"
            val place = gson.fromJson(placeJSON, Place::class.java)
            println(place)

            val dynamodb = getDynamoDBClient()
            println(dynamodb.describeTable("flapp_places"))
        } catch (e: Exception) {
            println("exception: $e")
        }
    }
}


class DynamoLambda {
    fun handler(input: InputStream, output: OutputStream): Unit {
        val gson = Gson()
        val inputAsString = input.bufferedReader().use { it.readText() }
        val place = gson.fromJson(inputAsString, Place::class.java)
        place.dummy = Random().nextInt()
        output.write(place.toString().toByteArray(charset("UTF-8")))

    }
}


/*
import boto3
dynamodb = boto3.resource("dynamodb", endpoint_url="http://localhost:8000/")
dynamodb.meta.client.list_tables()

aws dynamodb list-tables --endpoint-url http://localhost:8000

aws dynamodb create-table --endpoint-url http://localhost:8000 --table-name flapp_places --attribute-definitions AttributeName=h,AttributeType=S AttributeName=r,AttributeType=S --key-schema AttributeName=h,KeyType=HASH AttributeName=r,KeyType=RANGE --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
aws dynamodb delete-table --endpoint-url http://localhost:8000 --table-name flapp_places

aws lambda create-function --region us-east-1 --function-name dynamolambda --zip-file fileb://DynamoDBTest.jar --role arn:aws:iam::xxxxxx:role/lambda_basic_execution --handler DynamoLambda::handler --runtime java8 --timeout 15 --memory-size 128

aws lambda update-function-code --function-name dynamolambda --zip-file fileb://DynamoDBTest.jar

aws lambda invoke --function-name dynamolambda --payload '{"place": "lisbon","latitude": 1.11, "longitude": 2.22}' &>1 /tmp/output.txt;echo;cat /tmp/output.txt;echo
 */
