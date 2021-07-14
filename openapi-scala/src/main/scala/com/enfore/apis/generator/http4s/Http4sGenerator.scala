package com.enfore.apis.generator.http4s

import com.enfore.apis.ast.ASTTranslationFunctions.PackageName
import com.enfore.apis.generator.ScalaGenerator
import com.enfore.apis.generator.http4s.RouteGenerator.buildRefinementDecoders
import com.enfore.apis.repr.PathItemAggregation

object Http4sGenerator {

  /**
    * Case class used as input to create Scala source which holds an object for creating http4s.HttpRoutes
    */
  final case class RoutesObject(routesMap: Map[String, PathItemAggregation])

  /**
    * Case class used as input to create Scala source which holds a trait for the API implementation
    */
  final case class ApiTrait(routesMap: Map[String, PathItemAggregation])

  /**
    * The generated Routes object will look something like this:
    *
    * {{{
    * object Routes {
    *   def apply[F[_] : Sync](impl: Http4sRoutesApi[F]): HttpRoutes[F] = {
    *     val dsl = new Http4sDsl[F]{}
    *     import dsl._
    *
    *     HttpRoutes.of[F] {
    *       case GET -> Root / "contacts" / "individual" =>
    *         impl.`GET /contacts/individual`.flatMap(EntityGenerator(200)(_))
    *
    *       case request @ POST -> Root / "contacts" / "individual" =>
    *         request.as[IndividualContact].flatMap(impl.`POST /contacts/individual`).flatMap(EntityGenerator(200)(_))
    *     }
    *   }
    * }
    * }}}
    *
    * The implicit {{{Sync}}} parameter is needed for {{{HttpRoutes.of[F]}}}
    *
    * @param packageName The package where the Routes object should reside
    * @return A String with the Routes object's implementation as Scala Source
    */
  def routes(packageName: PackageName): ScalaGenerator[RoutesObject] = {
    case RoutesObject(routes) =>
      if (routes.isEmpty) {
        ""
      } else {
        s"""package ${packageName.name}
         |package http4s
         |
         |import cats.effect.Sync
         |import cats.implicits._
         |import org.http4s._
         |import io.circe.syntax._
         |import org.http4s.circe.CirceEntityEncoder._
         |import org.http4s.circe.CirceEntityDecoder._
         |import org.http4s.dsl.Http4sDsl
         |import com.enfore.apis.http4s._
         |
         |import eu.timepit.refined._
         |import eu.timepit.refined.api._
         |import eu.timepit.refined.collection._
         |import eu.timepit.refined.numeric._
         |import shapeless._
         |import eu.timepit.refined.boolean._
         |import io.circe.refined._
         |
         |object Http4sRoutes {
         |  def apply[F[_] : Sync](impl: Http4sRoutesApi[F], errorHandler: ErrorHandler[F]): HttpRoutes[F] = {
         |    new Http4sRoutes().routes
         |  }
         |}
         |
         |final class Http4sRoutes[F[_] : Sync](impl: Http4sRoutesApi[F], errorHandler: ErrorHandler[F]) extends Http4sDsl[F] {
         |${queryParameterMatchers(routes, indentationLevel = 2).mkString("\n")}
         |${routeDefinitions(routes, indentationLevel = 3)}
         |}
     """.stripMargin
      }
  }

  /**
    * The generated Http4sRoutesApi trait will look something like this:
    *
    * {{{
    * trait Http4sRoutesApi[F[_]] {
    *   def `GET /contacts/individual`: F[IndividualContact]
    *   def `POST /contacts/individual`(body: IndividualContact): F[IndividualContact]
    * }
    * }}}
    *
    * @param packageName The package where the Http4sRoutesApi trait should reside
    * @return A String with the Http4sRoutesApi trait's definition as Scala Source
    */
  def implementation(
      packageName: PackageName
  ): ScalaGenerator[ApiTrait] = {
    case ApiTrait(routes) =>
      s"""package ${packageName.name}
         |package http4s
         |
         |import org.http4s.Request
         |import eu.timepit.refined._
         |import eu.timepit.refined.api._
         |import eu.timepit.refined.collection._
         |import eu.timepit.refined.numeric._
         |import shapeless._
         |import eu.timepit.refined.boolean._
         |import io.circe.refined._
         |
         |trait Http4sRoutesApi[F[_]] {
         |${implementationTrait(routes, indentationLevel = 1).mkString("\n")}
         |}
      """.stripMargin
  }

  /**
    * Create query parameter decoder matchers from given routes if needed.
    * It will include a self-baked List[T] decoder, if any of the parameter types is a list
    * @param indentationLevel set the level of indentation for the scala source string
    */
  private def queryParameterMatchers(
      routes: Map[String, PathItemAggregation],
      indentationLevel: Int
  ): List[String] = {
    val routeDefinitions = routes.values.toList.flatMap(_.items)

    val refinementDecoders: List[String] = buildRefinementDecoders(routeDefinitions, indentationLevel)
    val listDecoder: List[String]        = RouteGenerator.listDecoder(routeDefinitions, indentationLevel)
    val enumDecoder: List[String]        = RouteGenerator.enumDecoders(routeDefinitions, indentationLevel)
    val matchers: List[String]           = RouteGenerator.buildMatchers(routeDefinitions, indentationLevel)
    val output                           = listDecoder ++ enumDecoder ++ (refinementDecoders :+ "\n") ++ matchers
    output.distinct
  }

  private def routeDefinitions(routes: Map[String, PathItemAggregation], indentationLevel: Int): String = {
    println(s"ROUTES:$routes")
    val indexedRoutes = routes.values.zipWithIndex
    val generated = indexedRoutes.toList.map { case (aggregation, i) =>
      val routes  = aggregation.items.map(RouteGenerator.generate(_, indentationLevel).mkString("\n"))
      s"""
         |val route${i+1} = HttpRoutes.of[F] {
         |  ${routes.mkString("\n\n")}
         |}
      """.stripMargin
    }
    println(s"GENERATED:$generated")
    val definedRoutes = (2 to generated.size).foldLeft("val routes: HttpRoutes[F] = route1") { case (acc, i) =>
      acc + s" <+> route$i"
    }
    println(s"DEFINED_ROUTES:$definedRoutes")

    val res = definedRoutes + generated.mkString("\n\n")
    println(s"RESSSS:$res")
    res
  }

  private def implementationTrait(routes: Map[String, PathItemAggregation], indentationLevel: Int): List[String] =
    routes.values.toList
      .flatMap(_.items)
      .map(ImplementationGenerator.generate(_, indentationLevel).mkString("\n"))
}
