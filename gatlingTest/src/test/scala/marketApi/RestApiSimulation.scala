package marketApi

import java.util.Locale
import java.util.concurrent.ThreadLocalRandom

import com.github.javafaker.Faker
import com.github.javafaker.service.{FakeValuesService, RandomService}
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class RestApiSimulation extends Simulation {

  private val userCount = getProperty("USERS", "10").toInt
  private val baseUrl = getProperty("URL", "")
  private val testDuration = getProperty("DURATION", "5").toInt
  private val maxOrderedProducts = getProperty("MAX_ORDERED_PRODUCTS", "20").toInt
  private val maxOrdersPerCustomer = getProperty("MAX_ORDERS", "1").toInt
  private val maxCategories = getProperty("MAX_CATEGORIES", "100").toInt

  private val fakeValuesService = new FakeValuesService(new Locale("en-US"), new RandomService)

  private def getProperty(propertyName: String, defaultValue: String) = {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  private def randomHumanName() = {
    new Faker().name().fullName()
  }

  private def randomEmail() = {
    fakeValuesService.bothify("????????????##@gmail.com")
  }

  private val customerFeeder = Iterator.continually(Map(
    "customerName" -> randomHumanName(),
    "customerEmail" -> randomEmail(),
  ))

  private val productIds = csv("data/products.csv").readRecords.map(record => record("productId"))

  private def prepareProductsForOrder(session: Session) = {
    session("customerEmail").validate[String].map { _ =>
      val orderedProductsCount = ThreadLocalRandom.current.nextInt(1, maxOrderedProducts)
      val selectedProductIds = Array.tabulate(orderedProductsCount)(_ =>
        productIds(ThreadLocalRandom.current.nextInt(productIds.length)))
      val quantity = ThreadLocalRandom.current.nextInt(1, 10)
      val productsJson = selectedProductIds.map(id => s""""$id": { "quantity": $quantity }""").mkString(",")
      session.set("productsJson", productsJson)
    }
  }

  private def customerFlow() = {
    feed(customerFeeder)
      .exec(session => prepareProductsForOrder(session))
      .exec(
        http("Add Customer")
          .post("/customer")
          .body(StringBody(
            """{
              |  "customerEmail": "${customerEmail}",
              |  "name": "${customerName}"
              |}""".stripMargin)).asJson
      )
      .exec(
        http("Get Customer")
          .get("/customer?id=${customerEmail}")
      )
      .exec(getProductsFlow())
      .exec(addOrdersFlow())
      .exec(
        http("Get Orders by Customer")
          .get("/orders?customerId=${customerEmail}")
      )
      .exec(
        http("Get Customer by OrderId")
          .get("/customer?orderId=${orderId}")
      )
  }

  private def getProductsFlow() = {
    repeat(ThreadLocalRandom.current.nextInt(1, 1 + maxOrdersPerCustomer)) {
      exec(session => prepareProductCategory(session))
      .exec(
        http("Get Products by Category")
          .get("/products?category=${categoryName}")
          .check(jsonPath("$[1].id").saveAs("productId"))
      )
      .exec(
        http("Get Products by Id")
          .get("/product?id=${productId}")
      )
    }
  }

  private def prepareProductCategory(session: Session) = {
    session("customerEmail").validate[String].map { _ =>
      val categoryId = ThreadLocalRandom.current.nextInt(1, 1 + maxCategories)
      val categoryName = s"Category$categoryId"
      session.set("categoryName", categoryName)
    }
  }

  private def addOrdersFlow() = {
    repeat(ThreadLocalRandom.current.nextInt(1, 1 + maxOrdersPerCustomer)) {
      exec(
        http("Add Order")
          .post("/order")
          .body(StringBody(
            """{
              |  "customerEmail": "${customerEmail}",
              |  "products": { ${productsJson} }
              |}""".stripMargin)).asJson
          .check(jsonPath("$.id").saveAs("orderId"))
          .check(jsonPath("$.total").saveAs("orderTotal"))
      )
      .exec(
        http("Get order")
          .get("/order?id=${orderId}")
      )
      .exec(
        http("Make payment")
          .post("/payment")
          .body(StringBody(
            """{
              |  "customerId": "${customerEmail}",
              |  "orderId": "${orderId}",
              |  "amount": ${orderTotal}
              |}""".stripMargin)).asJson
      )
      .exec(
        http("Update order status")
          .put("/order")
          .body(StringBody(
            """{
              |  "id": "${orderId}",
              |  "status": "DELIVERED"
              |}""".stripMargin)).asJson
      )
    }
  }

  private val scn = scenario("Default")
    .exec(
      customerFlow()
    )

  private val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")

  setUp(
    scn.inject(
      rampConcurrentUsers(1) to (userCount) during (2 * testDuration / 3 minutes),
      constantConcurrentUsers(userCount) during (testDuration / 3 minutes)
    )
    .protocols(httpProtocol)
  ).maxDuration(testDuration minutes)
}