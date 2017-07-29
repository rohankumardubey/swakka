package net.jtownson.swakka

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.testkit.{RouteTest, TestFrameworkInterface}
import net.jtownson.swakka.OpenApiJsonProtocol._
import net.jtownson.swakka.OpenApiModel._
import net.jtownson.swakka.RouteGen.openApiRoute
import net.jtownson.swakka.jsonschema.SchemaWriter._
import net.jtownson.swakka.model.Parameters.{BodyParameter, PathParameter, QueryParameter}
import net.jtownson.swakka.model.Responses.{Header, ResponseValue}
import net.jtownson.swakka.model.SecurityDefinitions.{ApiKeyInHeaderSecurity, Oauth2ImplicitSecurity, SecurityRequirement}
import net.jtownson.swakka.model.{ExternalDocs, Info, License, Tag}
import net.jtownson.swakka.routegen.ConvertibleToDirective._
import net.jtownson.swakka.routegen.{ConvertibleToDirective, SwaggerRouteSettings}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import shapeless.record._
import shapeless.syntax.singleton._
import shapeless.{::, HNil}
import spray.json._

class Petstore2Spec extends FlatSpec with RouteTest with TestFrameworkInterface {

  case class Pet(
                  id: Long,
                  name: String,
                  tag: Option[String] = None)

  type Pets = Seq[Pet]

  case class Error(
                    id: Int,
                    message: String
                  )

  implicit val petJsonFormat = jsonFormat3(Pet)
  implicit val petSchemaWriter = schemaWriter(Pet)
  implicit val petBodyParamConverter: ConvertibleToDirective[BodyParameter[Pet]] = bodyParamConverter[Pet]

  implicit val errorJsonFormat = jsonFormat2(Error)
  implicit val errorSchemaWriter = schemaWriter(Error)

  "Swakka" should "support the petstore v2 example, which includes auth" in {

    type ListPetsParams = QueryParameter[Int] :: HNil
    type ListPetsResponses = ResponseValue[Pets, Header[String]] :: ResponseValue[Error, HNil] :: HNil

    type CreatePetParams = BodyParameter[Pet] :: HNil
    type CreatePetResponses = ResponseValue[HNil, HNil] :: ResponseValue[HNil, HNil] :: ResponseValue[Error, HNil] :: HNil

    type ShowPetParams = PathParameter[String] :: HNil
    type ShowPetResponses = ResponseValue[Pets, HNil] :: ResponseValue[Error, HNil] :: HNil

    type Paths = PathItem[CreatePetParams, CreatePetResponses] :: HNil
//      PathItem[HNil, CreatePetResponses] :: PathItem[ShowPetParams, ShowPetResponses] :: HNil

    type SecurityDefinitions = Record.`'petstore_auth -> Oauth2ImplicitSecurity, 'api_key -> ApiKeyInHeaderSecurity`.T

    val securityDefinitions =
      'petstore_auth ->> Oauth2ImplicitSecurity(
        authorizationUrl = "http://petstore.swagger.io/oauth/dialog",
        scopes = Some(Map("write:pets" -> "modify pets in your account", "read:pets" -> "read your pets"))) ::
      'api_key ->> ApiKeyInHeaderSecurity("api_key") ::
      HNil


    val petstoreApi = OpenApi[Paths, SecurityDefinitions](
      info = Info(
        description = Some("This is a sample server Petstore server.  You can find out more about Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, you can use the api key `special-key` to test the authorization filters."),
        version = "1.0.0",
        title = "Swagger Petstore",
        licence = Some(License(
          name = "Apache 2.0",
          url = Some("http://www.apache.org/licenses/LICENSE-2.0.html"))
        ),
        termsOfService = Some("http://swagger.io/terms/")
      ),
      tags = Some(Seq(
        Tag(name = "pet",
          description = Some("Everything about your Pets"),
          externalDocs = Some(ExternalDocs(url = "http://swagger.io", description = Some("Find out more")))),
        Tag(name = "store",
          description = Some("Access to Petstore orders")),
        Tag(name = "user",
          description = Some("Operations about users"),
          externalDocs = Some(ExternalDocs(url = "http://swagger.io", description = Some("Find out more about our store")))))
      ),
      host = Some("petstore.swagger.io"),
      basePath = Some("/v2"),
      schemes = Some(Seq("http")),
      paths =
        PathItem[CreatePetParams, CreatePetResponses](
          path = "/pets",
          method = POST,
          operation = Operation(
            summary = Some("Add a new pet to the store"),
            description = Some(""),
            operationId = Some("addPet"),
            tags = Some(Seq("pets")),
            produces = Some(Seq("application/json", "application/xml")),
            consumes = Some(Seq("application/json", "application/xml")),
            parameters = BodyParameter[Pet]('body, Some("Pet object that needs to be added to the store")) :: HNil,
            responses =
              ResponseValue[HNil, HNil](
                responseCode = "201",
                description = "Pet added to the store"
              ) ::
              ResponseValue[HNil, HNil](
                responseCode = "405",
                description = "Invalid input"
              ) ::
              ResponseValue[Error, HNil](
                responseCode = "default",
                description = "unexpected error"
              ) ::
              HNil,
            security = Some(Seq(SecurityRequirement('petstore_auth, Seq("write:pets", "read:pets")))),
            endpointImplementation = _ => ???
          )
        ) :: HNil,
      securityDefinitions = Some(securityDefinitions)
    )

//          PathItem[ListPetsParams, ListPetsResponses](
//          path = "/pets",
//          method = GET,
//          operation = Operation(
//            summary = Some("List all pets"),
//            operationId = Some("listPets"),
//            tags = Some(Seq("pets")),
//            parameters =
//              QueryParameter[Int](
//                name = 'limit,
//                description = Some("How many items to return at one time (max 100)")) ::
//                HNil,
//            responses =
//              ResponseValue[Pets, Header[String]](
//                responseCode = "200",
//                description = "An paged array of pets",
//                headers = Header[String](Symbol("x-next"), Some("A link to the next page of responses"))) ::
//                ResponseValue[Error, HNil](
//                  responseCode = "default",
//                  description = "unexpected error"
//                ) :: HNil,
//            endpointImplementation = _ => ???)) ::
//          PathItem[ShowPetParams, ShowPetResponses](
//            path = "/pets/{petId}",
//            method = GET,
//            operation = Operation(
//              summary = Some("Info for a specific pet"),
//              operationId = Some("showPetById"),
//              tags = Some(Seq("pets")),
//              parameters =
//                PathParameter[String]('petId, Some("The id of the pet to retrieve")) ::
//                  HNil,
//              responses =
//                ResponseValue[Pets, HNil]("200", "Expected response to a valid request") ::
//                ResponseValue[Error, HNil]("default", "unexpected error") ::
//                HNil,
//              endpointImplementation = _ => ???
//            )
//          ) ::
//          HNil
//    )

    val apiRoutes = openApiRoute(petstoreApi, Some(SwaggerRouteSettings()))

    val expectedJson = JsObject(
      "swagger" -> JsString("2.0"),
      "info" -> JsObject(
        "description" -> JsString("This is a sample server Petstore server.  You can find out more about Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, you can use the api key `special-key` to test the authorization filters."),
        "title" -> JsString("Swagger Petstore"),
        "version" -> JsString("1.0.0"),
        "license" -> JsObject(
          "name" -> JsString("Apache 2.0"),
          "url" -> JsString("http://www.apache.org/licenses/LICENSE-2.0.html")
        ),
        "termsOfService" -> JsString("http://swagger.io/terms/")
      ),
      "host" -> JsString("petstore.swagger.io"),
      "basePath" -> JsString("/v2"),
      "schemes" -> JsArray(JsString("http")),
      "paths" -> JsObject(
        "/pets" -> JsObject(
//          "get" -> JsObject(
//            "summary" -> JsString("List all pets"),
//            "operationId" -> JsString("listPets"),
//            "tags" -> JsArray(
//              JsString("pets")
//            ),
//            "parameters" -> JsArray(
//              JsObject(
//                "name" -> JsString("limit"),
//                "in" -> JsString("query"),
//                "description" -> JsString("How many items to return at one time (max 100)"),
//                "required" -> JsBoolean(true),
//                "type" -> JsString("integer"),
//                "format" -> JsString("int32")
//              )
//            ),
//            "responses" -> JsObject(
//              "200" -> JsObject(
//                "description" -> JsString("An paged array of pets"),
//                "headers" -> JsObject(
//                  "x-next" -> JsObject(
//                    "type" -> JsString("string"),
//                    "description" -> JsString("A link to the next page of responses")
//                  )
//                ),
//                "schema" -> JsObject(
//                  "type" -> JsString("array"),
//                  "items" -> JsObject(
//                    "type" -> JsString("object"),
//                    "required" -> JsArray(JsString("id"), JsString("name")),
//                    "properties" -> JsObject(
//                      "id" -> JsObject(
//                        "type" -> JsString("integer"),
//                        "format" -> JsString("int64")),
//                      "name" -> JsObject(
//                        "type" -> JsString("string")),
//                      "tag" -> JsObject(
//                        "type" -> JsString("string"))
//                    )
//                  )
//                )
//              ),
//              "default" -> JsObject(
//                "description" -> JsString("unexpected error"),
//                "schema" -> JsObject(
//                  "type" -> JsString("object"),
//                  "required" -> JsArray(JsString("id"), JsString("message")),
//                  "properties" -> JsObject(
//                    "id" -> JsObject(
//                      "type" -> JsString("integer"),
//                      "format" -> JsString("int32")
//                    ),
//                    "message" -> JsObject(
//                      "type" -> JsString("string")
//                    )
//                  )
//                )
//              )
//            )
//          ),
          "post" -> JsObject(
            "summary" -> JsString("Add a new pet to the store"),
            "operationId" -> JsString("addPet"),
            "tags" -> JsArray(JsString("pets")),
            "parameters" -> JsArray(
              JsObject(
                "in" -> JsString("body"),
                "name" -> JsString("body"),
                "description" -> JsString("Pet object that needs to be added to the store"),
                "required" -> JsBoolean(true),
                "schema" -> JsObject(
                  "type" -> JsString("object"),
                  "required" -> JsArray(JsString("id"), JsString("name")),
                  "properties" -> JsObject(
                    "id" -> JsObject(
                      "type" -> JsString("integer"),
                      "format" -> JsString("int64")),
                    "name" -> JsObject(
                      "type" -> JsString("string")),
                    "tag" -> JsObject(
                      "type" -> JsString("string"))
                  )
                )
              )
            ),
            "responses" -> JsObject(
              "201" -> JsObject(
                "description" -> JsString("Pet added to the store")
              ),
              "405" -> JsObject(
                "description" -> JsString("Invalid input")
              ),
              "default" -> JsObject(
                "description" -> JsString("unexpected error"),
                "schema" -> JsObject(
                  "type" -> JsString("object"),
                  "required" -> JsArray(JsString("id"), JsString("message")),
                  "properties" -> JsObject(
                    "id" -> JsObject(
                      "type" -> JsString("integer"),
                      "format" -> JsString("int32")
                    ),
                    "message" -> JsObject(
                      "type" -> JsString("string")
                    )
                  )
                )
              )
            )
          )
        ) //,
//        "/pets/{petId}" -> JsObject(
//          "get" -> JsObject(
//            "summary" -> JsString("Info for a specific pet"),
//            "operationId" -> JsString("showPetById"),
//            "tags" -> JsArray(
//              JsString("pets")
//            ),
//            "parameters" -> JsArray(
//              JsObject(
//                "name" -> JsString("petId"),
//                "in" -> JsString("path"),
//                "required" -> JsBoolean(true),
//                "description" -> JsString("The id of the pet to retrieve"),
//                "type" -> JsString("string")
//              )
//            ),
//            "responses" -> JsObject(
//              "200" -> JsObject(
//                "description" -> JsString("Expected response to a valid request"),
//                "schema" -> JsObject(
//                  "type" -> JsString("array"),
//                  "items" -> JsObject(
//                    "type" -> JsString("object"),
//                    "required" -> JsArray(JsString("id"), JsString("name")),
//                    "properties" -> JsObject(
//                      "id" -> JsObject(
//                        "type" -> JsString("integer"),
//                        "format" -> JsString("int64")),
//                      "name" -> JsObject(
//                        "type" -> JsString("string")),
//                      "tag" -> JsObject(
//                        "type" -> JsString("string"))
//                    )
//                  )
//                )
//              ),
//              "default" -> JsObject(
//                "description" -> JsString("unexpected error"),
//                "schema" -> JsObject(
//                  "type" -> JsString("object"),
//                  "required" -> JsArray(JsString("id"), JsString("message")),
//                  "properties" -> JsObject(
//                    "id" -> JsObject(
//                      "type" -> JsString("integer"),
//                      "format" -> JsString("int32")
//                    ),
//                    "message" -> JsObject(
//                      "type" -> JsString("string")
//                    )
//                  )
//                )
//              )
//            )
//          )
//        )
      )
    )
//
    Get("http://petstore.swagger.io/v2/swagger.json") ~> apiRoutes ~> check {
      JsonParser(responseAs[String]) shouldBe expectedJson
    }
  }

  override def failTest(msg: String): Nothing = throw new AssertionError(msg)
}
