package ru.tinkoff.tschema.finagle.routing.impl

import ru.tinkoff.tschema.finagle.LiftHttp
import ru.tinkoff.tschema.finagle.routing.{Fail, ZioRouting}
import ru.tinkoff.tschema.finagle.routing.ZioRouting.ZIOHttp
import zio.ZIO

private[routing] class ZioLiftInstance[R, R1, E, E1](implicit eve: E1 <:< E, evr: R <:< R1)
    extends LiftHttp[ZIOHttp[R, E, *], ZIO[R1, E1, *]] {
  private type F[a] = ZIOHttp[R, E, a]
  def apply[A](fa: ZIO[R1, E1, A]): F[A] = fa.mapError(Fail.Other(_): Fail[E]).provideSome[ZioRouting[R]](_.embedded)
}
