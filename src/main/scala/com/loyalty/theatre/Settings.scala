package com.loyalty.theatre

import akka.actor.ActorSystem
import com.typesafe.config.Config

class Settings(config: Config) {
  def this(system: ActorSystem) = this(system.settings.config)

  object cluster {
    val numberOfShards: Int = config.getInt("app.number-of-shards")
  }

  object server {
    val host: String = config.getString("app.http.host")
    val port: Int = config.getString("app.http.port").toInt
    val context: String = config.getString("app.http.context")
  }
}

object Settings {
  def apply(system: ActorSystem): Settings = new Settings(system)

  def apply(config: Config): Settings = new Settings(config)
}

