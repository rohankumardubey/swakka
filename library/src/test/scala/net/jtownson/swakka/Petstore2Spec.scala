package net.jtownson.swakka

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpMethods.{POST, PUT}
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

//    type ListPetsParams = QueryParameter[Int] :: HNil
//    type ListPetsResponses = ResponseValue[Pets, Header[String]] :: ResponseValue[Error, HNil] :: HNil

    //    type ShowPetParams = PathParameter[String] :: HNil
    //    type ShowPetResponses = ResponseValue[Pets, HNil] :: ResponseValue[Error, HNil] :: HNil

    type CreatePetParams = BodyParameter[Pet] :: HNil
    type CreatePetResponses = ResponseValue[HNil, HNil] :: ResponseValue[HNil, HNil] :: ResponseValue[Error, HNil] :: HNil

    type UpdatePetParams = BodyParameter[Pet] :: HNil
    type UpdatePetResponses = ResponseValue[HNil, HNil] :: ResponseValue[HNil, HNil] :: ResponseValue[HNil, HNil] :: HNil

//    type FindByStatusParams = QueryParameter[] :: HNil

    type Paths = PathItem[CreatePetParams, CreatePetResponses] :: PathItem[UpdatePetParams, UpdatePetResponses] :: HNil
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
            consumes = Some(Seq("application/json", "application/xml")),
            produces = Some(Seq("application/xml", "application/json")),
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
        ) ::
        PathItem[UpdatePetParams, UpdatePetResponses](
          path = "/pets",
          method = PUT,
          operation = Operation(
            summary = Some("Update an existing pet"),
            description = Some(""),
            operationId = Some("updatePet"),
            tags = Some(Seq("pet")),
            consumes = Some(Seq("application/json", "application/xml")),
            produces = Some(Seq("application/xml", "application/json")),
            parameters = BodyParameter[Pet]('body, Some("Pet object that needs to be added to the store")) :: HNil,
            responses =
              ResponseValue[HNil, HNil](
                responseCode = "400",
                description = "Invalid ID supplied"
              ) ::
              ResponseValue[HNil, HNil](
                responseCode = "404",
                description = "Pet not found"
              ) ::
              ResponseValue[HNil, HNil](
                responseCode = "405",
                description = "Validation exception"
              ) ::
              HNil,
            security = Some(Seq(SecurityRequirement('petstore_auth, Seq("write:pets", "read:pets")))),
            endpointImplementation = _ => ???
          )
        )
        :: HNil,
      securityDefinitions = Some(securityDefinitions)
    )

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
          "post" -> JsObject(
            "summary" -> JsString("Add a new pet to the store"),
            "description" -> JsString(""),
            "operationId" -> JsString("addPet"),
            "tags" -> JsArray(JsString("pets")),
            "consumes" -> JsArray(JsString("application/json"), JsString("application/xml")),
            "produces" -> JsArray(JsString("application/xml"), JsString("application/json")),
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
            ),
            "security" -> JsArray(
              JsObject(
                "petstore_auth" -> JsArray(JsString("write:pets"), JsString("read:pets")
                )
              )
            )
          ),
          "put" -> JsObject(
            "tags" -> JsArray(JsString("pet")),
            "summary" -> JsString("Update an existing pet"),
            "description" -> JsString(""),
            "operationId" -> JsString("updatePet"),
            "consumes" -> JsArray(JsString("application/json"), JsString("application/xml")),
            "produces" -> JsArray(JsString("application/xml"), JsString("application/json")),
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
              "400" -> JsObject(
                "description" -> JsString("Invalid ID supplied")
              ),
              "404" -> JsObject(
                "description" -> JsString("Pet not found")
              ),
              "405" -> JsObject(
                "description" -> JsString("Validation exception")
              )
            ),
            "security" -> JsArray(
              JsObject(
                "petstore_auth" -> JsArray(JsString("write:pets"), JsString("read:pets")
                )
              )
            )
          )
        )/* TODO enable after enum support added
        ,
        "/pet/findByStatus" -> JsObject(
          "get" -> JsObject(
            "tags" -> JsArray(JsString("pet")),
            "summary" -> JsString("Finds Pets by status"),
            "description" -> JsString("Multiple status values can be provided with comma separated strings"),
            "operationId" -> JsString("findPetsByStatus"),
            "produces" -> JsArray(
              JsString("application/xml"),
              JsString("application/json")),
            "parameters" -> JsArray(
              JsObject(
                "name" -> JsString("status"),
                "in" -> JsString("query"),
                "description" -> JsString("Status values that need to be considered for filter"),
                "required" -> JsBoolean(true),
                "type" -> JsString("array"),
                "items" -> JsObject(
                  "type" -> JsString("string"),
                  "enum" -> JsArray(
                    JsString("available"),
                    JsString("pending"),
                    JsString("sold")
                  ),
                  "default" -> JsString("available")
                ),
                "collectionFormat" -> JsString("multi")
              )
            ),
            "responses" -> JsObject(
              "200" -> JsObject(
                "description" -> JsString("successful operation"),
                "schema" -> JsObject(
                  "type" -> JsString("array"),
                  "items" -> JsObject(
                    "$ref" -> JsString("#/definitions/Pet"))
                )
              ),
              "400" -> JsObject(
                "description" -> JsString("Invalid status value")
              )
            ),
            "security" -> JsArray(
              JsObject(
                "petstore_auth" -> JsArray(JsString("write:pets"), JsString("read:pets"))
              )
            )
          )
          */
        )
      )

    //
    Get("http://petstore.swagger.io/v2/swagger.json") ~> apiRoutes ~> check {
      JsonParser(responseAs[String]) shouldBe expectedJson
    }
  }

  override def failTest(msg: String): Nothing = throw new AssertionError(msg)
}
