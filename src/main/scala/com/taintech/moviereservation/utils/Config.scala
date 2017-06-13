package com.taintech.moviereservation.utils

import com.typesafe.config.ConfigFactory

trait Config {
  private val config = ConfigFactory.load()
  private val httpConfig = config.getConfig("http")

  val httpHost: String = httpConfig.getString("interface")
  val httpPort: Int = httpConfig.getInt("port")

}
