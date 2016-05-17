package com.hornetdevelopment.diyha.config

import com.typesafe.config.ConfigFactory

import scala.util.Try

/**
  * Created by steve on 5/11/16.
  */
trait Config {
  private val conf = ConfigFactory.load()

  def getConfigString(key: String, defaultValue: String): String = {
    Try(conf.getString(key)).getOrElse(defaultValue)
  }

  def getConfigInt(key: String, defaultValue: Int): Int = {
    Try(conf.getInt(key)).getOrElse(defaultValue)
  }
}
