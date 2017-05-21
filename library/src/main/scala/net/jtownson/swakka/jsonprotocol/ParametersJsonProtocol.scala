package net.jtownson.swakka.jsonprotocol

import net.jtownson.swakka.jsonschema.ApiModelDictionary._
import net.jtownson.swakka.jsonschema.{JsonSchema, SchemaWriter}
import shapeless.{::, HList, HNil}
import spray.json.{JsArray, JsBoolean, JsFalse, JsObject, JsString, JsValue}
import ParameterJsonFormat.func2Format
import net.jtownson.swakka.model.Parameters.{BodyParameter, HeaderParameter, PathParameter, QueryParameter}

import scala.reflect.runtime.universe.TypeTag

trait ParametersJsonProtocol {

  implicit val strQueryParamFormat: ParameterJsonFormat[QueryParameter[String]] =
    (qp: QueryParameter[String]) => simpleParam(qp.name, "query", qp.description, qp.required, "string", None)

  implicit val floatQueryParamFormat: ParameterJsonFormat[QueryParameter[Float]] =
    (qp: QueryParameter[Float]) => simpleParam(qp.name, "query", qp.description, qp.required, "number", Some("float"))

  implicit val doubleQueryParamFormat: ParameterJsonFormat[QueryParameter[Double]] =
    (qp: QueryParameter[Double]) => simpleParam(qp.name, "query", qp.description, qp.required, "number", Some("double"))

  implicit val booleanQueryParamFormat: ParameterJsonFormat[QueryParameter[Boolean]] =
    (qp: QueryParameter[Boolean]) => simpleParam(qp.name, "query", qp.description, qp.required, "boolean", None)

  implicit val intQueryParamFormat: ParameterJsonFormat[QueryParameter[Int]] =
    (qp: QueryParameter[Int]) => simpleParam(qp.name, "query", qp.description, qp.required, "integer", Some("int32"))

  implicit val longQueryParamFormat: ParameterJsonFormat[QueryParameter[Long]] =
    (qp: QueryParameter[Long]) => simpleParam(qp.name, "query", qp.description, qp.required, "integer", Some("int64"))

  implicit val strPathParamFormat: ParameterJsonFormat[PathParameter[String]] =
    (pp: PathParameter[String]) => simpleParam(pp.name, "path", pp.description, pp.required, "string", None)

  implicit val floatPathParamFormat: ParameterJsonFormat[PathParameter[Float]] =
    (pp: PathParameter[Float]) => simpleParam(pp.name, "path", pp.description, pp.required, "number", Some("float"))

  implicit val doublePathParamFormat: ParameterJsonFormat[PathParameter[Double]] =
    (pp: PathParameter[Double]) => simpleParam(pp.name, "path", pp.description, pp.required, "number", Some("double"))

  implicit val booleanPathParamFormat: ParameterJsonFormat[PathParameter[Boolean]] =
    (pp: PathParameter[Boolean]) => simpleParam(pp.name, "path", pp.description, pp.required, "boolean", None)

  implicit val intPathParamFormat: ParameterJsonFormat[PathParameter[Int]] =
    (pp: PathParameter[Int]) => simpleParam(pp.name, "path", pp.description, pp.required, "integer", Some("int32"))

  implicit val longPathParamFormat: ParameterJsonFormat[PathParameter[Long]] =
    (pp: PathParameter[Long]) => simpleParam(pp.name, "path", pp.description, pp.required, "integer", Some("int64"))

  implicit val strHeaderParamFormat: ParameterJsonFormat[HeaderParameter[String]] =
    (hp: HeaderParameter[String]) => simpleParam(hp.name, "header", hp.description, hp.required, "string", None)

  implicit val floatHeaderParamFormat: ParameterJsonFormat[HeaderParameter[Float]] =
    (hp: HeaderParameter[Float]) => simpleParam(hp.name, "header", hp.description, hp.required, "number", Some("float"))

  implicit val doubleHeaderParamFormat: ParameterJsonFormat[HeaderParameter[Double]] =
    (hp: HeaderParameter[Double]) => simpleParam(hp.name, "header", hp.description, hp.required, "number", Some("double"))

  implicit val booleanHeaderParamFormat: ParameterJsonFormat[HeaderParameter[Boolean]] =
    (hp: HeaderParameter[Boolean]) => simpleParam(hp.name, "header", hp.description, hp.required, "boolean", None)

  implicit val intHeaderParamFormat: ParameterJsonFormat[HeaderParameter[Int]] =
    (hp: HeaderParameter[Int]) => simpleParam(hp.name, "header", hp.description, hp.required, "integer", Some("int32"))

  implicit val longHeaderParamFormat: ParameterJsonFormat[HeaderParameter[Long]] =
    (hp: HeaderParameter[Long]) => simpleParam(hp.name, "header", hp.description, hp.required, "integer", Some("int64"))

  implicit def bodyParamFormat[T: TypeTag](implicit ev: SchemaWriter[T]): ParameterJsonFormat[BodyParameter[T]] = {

    implicit val dict = apiModelDictionary[T]

    func2Format((bp: BodyParameter[T]) => JsObject(
      "name" -> JsString(bp.name.name),
      "in" -> JsString("body"),
      "description" -> JsString(""),
      "required" -> JsFalse,
      "schema" -> ev.write(JsonSchema[T]())
    ))
  }

  implicit val hNilParamFormat: ParameterJsonFormat[HNil] =
    _ => JsArray()

  implicit def hConsParamFormat[H, T <: HList](implicit head: ParameterJsonFormat[H], tail: ParameterJsonFormat[T]): ParameterJsonFormat[H :: T] =
    func2Format((l: H :: T) => {
      Flattener.flattenToArray(JsArray(head.write(l.head), tail.write(l.tail)))
    })

  private def simpleParam(name: Symbol, in: String, description: Option[String], required: Boolean, `type`: String, format: Option[String]): JsValue =
    JsObject(List(
      Some("name" -> JsString(name.name)),
      Some("in" -> JsString(in)),
      description.map("description" -> JsString(_)),
      Some("required" -> JsBoolean(required)),
      Some("type" -> JsString(`type`)),
      format.map("format" -> JsString(_))).flatten: _*
    )

}

object ParametersJsonProtocol extends ParametersJsonProtocol