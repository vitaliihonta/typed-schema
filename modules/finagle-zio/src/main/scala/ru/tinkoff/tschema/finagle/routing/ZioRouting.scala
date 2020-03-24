package ru.tinkoff.tschema.finagle.routing

import com.twitter.finagle.http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import ru.tinkoff.tschema.finagle.routing.ZioRouting.ZIOHttp
import ru.tinkoff.tschema.finagle.routing.impl.{ZIOConvertService, ZioLiftInstance, ZioRoutedInstance}
import ru.tinkoff.tschema.finagle.routing.zioRouting.execResponse
import ru.tinkoff.tschema.finagle._
import ru.tinkoff.tschema.utils.SubString
import zio.{URIO, ZIO}

final case class ZioRouting[@specialized(Unit) +R](
    request: http.Request,
    path: CharSequence,
    matched: Int,
    embedded: R
)

object ZioRouting extends ZIORoutedPlusInstances {

  type ZIOHttp[-R, +E, +A] = ZIO[ZioRouting[R], Fail[E], A]
  type URIOHttp[-R, +A]    = ZIO[ZioRouting[R], Fail[Nothing], A]

  implicit def zioUnexceptRouted[R]: RoutedPlus[URIOHttp[R, *]] =
    zioRoutedAny.asInstanceOf[ZioRoutedInstance[R, Nothing]]

  implicit def zioUnexceptLift[R, R1](
      implicit evr: R <:< R1
  ): LiftHttp[URIOHttp[R, *], URIO[R1, *]] =
    zioLiftAny.asInstanceOf[ZioLiftInstance[R, R1, Nothing, Nothing]]

  implicit def zioConvertService[R, E]: ConvertService[ZIOHttp[R, E, *]] =
    zioConvertServiceAny.asInstanceOf[ConvertService[ZIOHttp[R, E, *]]]

  implicit def zioRunnable[R, E <: Throwable](
      implicit
      rejectionHandler: Rejection.Handler = Rejection.defaultHandler
  ): RunHttp[ZIOHttp[R, E, *], ZIO[R, E, *]] =
    zioResponse => ZIO.runtime[R].flatMap(runtime => ZIO.effectTotal(exec(runtime, zioResponse, _)))

  private[this] def exec[R, E <: Throwable](
      runtime: zio.Runtime[R],
      zioResponse: ZIOHttp[R, E, Response],
      request: Request
  )(implicit handler: Rejection.Handler): Future[Response] =
    execResponse[R, ZioRouting[R], E](runtime, zioResponse, ZioRouting(request, SubString(request.path), 0, _))
}

trait ZIORoutedPlusInstances {
  implicit def zioRouted[R, E]: RoutedPlus[ZIOHttp[R, E, *]] =
    zioRoutedAny.asInstanceOf[ZioRoutedInstance[R, E]]

  implicit def zioLift[R, R1, E, E1](
      implicit eve: E1 <:< E,
      evr: R <:< R1
  ): LiftHttp[ZIOHttp[R, E, *], ZIO[R1, E1, *]] =
    zioLiftAny.asInstanceOf[ZioLiftInstance[R, R1, E, E1]]

  final protected[this] object zioRoutedAny         extends ZioRoutedInstance[Any, Nothing]
  final protected[this] object zioLiftAny           extends ZioLiftInstance[Any, Any, Nothing, Nothing]
  final protected[this] object zioConvertServiceAny extends ZIOConvertService[Any, Nothing]

}
