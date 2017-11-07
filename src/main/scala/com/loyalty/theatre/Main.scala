package com.loyalty.theatre

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.loyalty.theatre.actors.MovieTheatre
import com.loyalty.theatre.actors.sharding.MovieTheatreSettings
import com.loyalty.theatre.http.Routes
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main extends App with Routes {
  val config = ConfigFactory.load()
  implicit val system: ActorSystem = ActorSystem(config.getString("app.name"), config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  val settings = Settings(system)

  override val theatreCoordinator = ClusterSharding(system).start(
    typeName = MovieTheatreSettings.shardName,
    entityProps = MovieTheatre.props(),
    settings = ClusterShardingSettings(system),
    extractEntityId = MovieTheatreSettings.extractEntityId,
    extractShardId = MovieTheatreSettings.extractShardId(settings.cluster.numberOfShards)
  )
  override implicit val timeout: Timeout = Timeout(5.seconds)
  override val log: LoggingAdapter = system.log

  val prefixedRoutes =
    if (settings.server.context.nonEmpty) pathPrefix(settings.server.context)(routes)
    else routes

  Http().bindAndHandle(prefixedRoutes, settings.server.host, settings.server.port).onComplete {
    case Success(binding) =>
      log.info("Server online at http://{}:{}", binding.localAddress.getHostName, binding.localAddress.getPort)

    case Failure(ex) =>
      log.error(ex, "Failed to start server. Shutting down actor system")
      system.terminate()
  }
}
