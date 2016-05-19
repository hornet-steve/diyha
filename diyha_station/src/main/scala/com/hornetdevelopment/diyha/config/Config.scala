package com.hornetdevelopment.diyha.config

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

/**
  * Created by steve on 5/11/16.
  */
trait Config extends LazyLogging {
  private val conf = ConfigFactory.load()

  def getConfigString(key: String, defaultValue: String): String = {
    Try(conf.getString(key)).getOrElse {
      logger.warn(s"Unable to find String config value for key: ${key} - using default value: ${defaultValue}")
      defaultValue
    }
  }

  def getConfigInt(key: String, defaultValue: Int): Int = {
    Try(conf.getInt(key)).getOrElse {
      logger.warn(s"Unable to find Int config value for key: ${key} - using default value: ${defaultValue}")
      defaultValue
    }
  }
}
