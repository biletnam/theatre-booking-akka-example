package com.loyalty.theatre.actors.sharding

import akka.cluster.sharding.ShardRegion
import com.loyalty.theatre.actors.MovieTheatre

object MovieTheatreSettings {
  case class MovieTheatreEnvelope(id: String, command: MovieTheatre.Command)

  val shardName: String = "MovieTheatreShard"
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case e: MovieTheatreEnvelope => (e.id, e.command)
  }

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case e: MovieTheatreEnvelope => (Math.abs(e.id.hashCode) % numberOfShards).toString
    case ShardRegion.StartEntity(id) => (id.toLong           % numberOfShards).toString
  }
}
