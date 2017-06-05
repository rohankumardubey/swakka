import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.complete
import akka.stream.ActorMaterializer
import net.jtownson.swakka.routegen.{CorsUseCases, SwaggerRouteSettings}
import shapeless.{::, HNil}

import scala.collection.immutable.Seq


// Shows how to create
// 1. a simple API, with a single endpoint that takes no parameters
// 2. the corresponding akka route
// 3. the swagger file endpoint at /swagger.json

// Usage: curl -i http://localhost:8080/ping

// Core OpenAPI case classes
import net.jtownson.swakka.OpenApiModel._
import net.jtownson.swakka.model.Responses.ResponseValue

// Generates an akka-http Route from an API definition
import net.jtownson.swakka.RouteGen

// Implicit json formats for serializing the swagger.json
import net.jtownson.swakka.OpenApiJsonProtocol._


object PingPong extends App {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  type NoParams = HNil
  type StringResponse = ResponseValue[String, HNil]

  type Paths = PathItem[NoParams, StringResponse] :: HNil

  val corsHeaders = Seq(
    RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Methods", "GET"))

  val api =
    OpenApi(
      produces = Some(Seq("text/plain")),
      paths =
      PathItem[NoParams, StringResponse](
        path = "/ping",
        method = GET,
        operation = Operation[NoParams, StringResponse](
          responses = ResponseValue[String, HNil]("200", "ok"),
          endpointImplementation = _ => complete(HttpResponse(OK, corsHeaders, "pong"))
        )
      ) ::
        HNil
    )

  val route: Route = RouteGen.openApiRoute(api, Some(SwaggerRouteSettings(
    corsUseCase = CorsUseCases.SpecificallyThese(corsHeaders))))

  val bindingFuture = Http().bindAndHandle(
    route,
    "localhost",
    8080)
}
