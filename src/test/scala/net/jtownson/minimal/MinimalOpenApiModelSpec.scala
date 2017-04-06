package net.jtownson.minimal

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.MalformedQueryParamRejection
import akka.http.scaladsl.testkit.{RouteTest, TestFrameworkInterface}
import net.jtownson.minimal.ConvertibleToDirective0._
import net.jtownson.minimal.MinimalOpenApiModel._
import net.jtownson.minimal.RouteGen._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec
import org.scalatest.Inside._
import org.scalatest.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._
import shapeless.{HNil, ::}

class MinimalOpenApiModelSpec extends FlatSpec with MockFactory with RouteTest with TestFrameworkInterface {

  val f = mockFunction[HttpRequest, ToResponseMarshallable]

  private val defaultItem = PathItem[HNil, ResponseValue[String] :: HNil](GET, Operation(HNil, ResponseValue[String](200) :: HNil, f))

  val zeroParamModels = Table(
    ("testcase name", "request", "model", "response"),
    ("index page", get("/"), OpenApiModel("/", defaultItem), "YES"),
    ("simple path", get("/ruok"), OpenApiModel("/ruok", defaultItem), "YES"),
    ("missing base path", get("/ruok"), OpenApiModel("ruok", defaultItem), "YES"),
    ("complex path", get("/ruok/json"), OpenApiModel("ruok/json", defaultItem), "YES")
  )

  forAll(zeroParamModels) { (testcaseName, request, apiModel, response) =>
    testcaseName should "convert to a complete akka Route" in {

      f expects request returning response

      val route = openApiRoute(apiModel)

      request ~> route ~> check {
        status shouldBe OK
        responseAs[String] shouldBe response
      }
    }
  }

  type OneStringParam = QueryParameter[String] :: HNil

  private val itemWithQueryParam = PathItem[OneStringParam, ResponseValue[String] :: HNil](
    GET, Operation(QueryParameter[String]('q) :: HNil, ResponseValue[String](200) :: HNil, f))

  val oneStrParamModels = Table(
    ("testcase name", "request", "model", "response"),
    ("echo query", get("/app?q=x"), OpenApiModel("/app", itemWithQueryParam), "x")
  )

  forAll(oneStrParamModels) { (testcaseName, request, apiModel, response) =>
    testcaseName should "convert to a complete akka Route" in {

      f expects request returning response

      val route = openApiRoute(apiModel)

      request ~> route ~> check {
        status shouldBe OK
        responseAs[String] shouldBe response
      }
    }
  }

  type OneIntParam = QueryParameter[Int] :: HNil

  val itemWithIntParam = PathItem[OneIntParam, ResponseValue[String] :: HNil](
    GET, Operation(QueryParameter[Int]('q) :: HNil, ResponseValue[String](200) :: HNil, f))

  "int params that are NOT ints" should "be rejected" in {

    val request = get("/app?q=x")

    val route = openApiRoute(OpenApiModel("/app", itemWithIntParam))

    request ~> route ~> check {
      inside (rejection) { case MalformedQueryParamRejection(parameterName, _, _) =>
        parameterName shouldBe "q"
      }
    }
  }

  "int params that are ints" should "be passed through" in {

    val request = get("/app?q=10")

    f expects request returning "x"

    val route = openApiRoute(OpenApiModel("/app", itemWithIntParam))

    request ~> route ~> check {
      status shouldBe OK
      responseAs[String] shouldBe "x"
    }
  }

  private def get(path: String): HttpRequest = {
    Get(s"http://example.com$path")
  }

  override def failTest(msg: String): Nothing = throw new AssertionError(msg)
}
