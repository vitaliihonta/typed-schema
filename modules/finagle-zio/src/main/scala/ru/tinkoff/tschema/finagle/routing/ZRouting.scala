package ru.tinkoff.tschema.finagle.routing

import cats.evidence.As
import com.twitter.finagle.http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import ru.tinkoff.tschema.finagle._
import ru.tinkoff.tschema.finagle.routing.impl.{ZiosConvertService, ZiosLiftInstance, ZiosRoutedInstance}
import ru.tinkoff.tschema.finagle.routing.zioRouting.{HasRouting, ZIOH, execResponse}
import ru.tinkoff.tschema.utils.SubString
import zio.{Has, ZIO}

final case class ZRouting(
    request: http.Request,
    path: CharSequence,
    matched: Int
)

object ZRouting {

  implicit def ziosRouted[R, E]: RoutedPlus[ZIOH[R, E, *]] =
    ziosRoutedAny.asInstanceOf[ZiosRoutedInstance[R, E]]

  implicit def ziosLift[R, R1, E, E1](implicit asE: E1 <:< E, asR: R As R1): LiftHttp[ZIOH[R, E, *], ZIO[R1, E1, *]] =
    ziosLiftAny.asInstanceOf[ZiosLiftInstance[R, R1, E, E1]]

  def ziosConvertService[R, E]: ConvertService[ZIOH[R, E, *]] =
    ziosConvertAny.asInstanceOf[ConvertService[ZIOH[R, E, *]]]

  implicit def ziosRunnable[R <: Has[_], E <: Throwable](
      implicit
      rejectionHandler: Rejection.Handler = Rejection.defaultHandler
  ): RunHttp[ZIOH[R, E, *], ZIO[R, E, *]] =
    zioResponse => ZIO.runtime[R].flatMap(runtime => ZIO.effectTotal(exec(runtime, zioResponse, _)))

  private[this] def exec[R <: Has[_], E <: Throwable](
      runtime: zio.Runtime[R],
      zioResponse: ZIOH[R, E, Response],
      request: Request
  )(implicit handler: Rejection.Handler): Future[Response] =
    execResponse[R, R with HasRouting, E](runtime, zioResponse, _ add ZRouting(request, SubString(request.path), 0))

  private[this] object ziosRoutedAny  extends ZiosRoutedInstance[Any, Nothing]
  private[this] object ziosLiftAny    extends ZiosLiftInstance[Any, Any, Nothing, Nothing]
  private[this] object ziosConvertAny extends ZiosConvertService[Any, Nothing]
}
