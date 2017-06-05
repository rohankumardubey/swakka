import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import net.jtownson.swakka.OpenApiJsonProtocol._
import net.jtownson.swakka.OpenApiModel._
import net.jtownson.swakka.RouteGen
import net.jtownson.swakka.model.Parameters.PathParameter
import net.jtownson.swakka.model.Responses.ResponseValue
import net.jtownson.swakka.routegen.CorsUseCases.SpecificallyThese
import net.jtownson.swakka.routegen.SwaggerRouteSettings
import shapeless.{::, HNil}

import scala.collection.immutable.Seq

// Shows how to create
// an endpoint that accepts a path parameter

// Usage: curl -i http://localhost:8080/greet/John

object Greeter2 extends App {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  type Params = PathParameter[String] :: HNil
  type StringResponse = ResponseValue[String, HNil]

  type Paths = PathItem[Params, StringResponse] :: HNil

  val corsHeaders = Seq(
    RawHeader("Access-Control-Allow-Origin", "*"),
    RawHeader("Access-Control-Allow-Methods", "GET"))

  val greet: Params => Route = {
    case (PathParameter(name) :: HNil) =>
      complete(HttpResponse(OK, corsHeaders, s"Hello ${name}!"))
  }

  val api =
    OpenApi(
      produces = Some(Seq("text/plain")),
      paths =
      PathItem(
        path = "/greet/{name}",
        method = GET,
        operation = Operation[Params, StringResponse](
          parameters = PathParameter[String]('name) :: HNil,
          responses = ResponseValue[String, HNil]("200", "ok"),
          endpointImplementation = greet
        )
      ) ::
        HNil
    )

  val route: Route = RouteGen.openApiRoute(
    api,
    Some(SwaggerRouteSettings(
      corsUseCase = SpecificallyThese(corsHeaders))))

  val bindingFuture = Http().bindAndHandle(
    route,
    "localhost",
    8080)
}
