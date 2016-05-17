package com.hornetdevelopment.diyha

import java.text.SimpleDateFormat
import java.util.Date

import com.datastax.driver.core.BoundStatement
import com.hornetdevelopment.diyha.cassandra.CassandraClient
import com.hornetdevelopment.diyha.config.Config
import com.hornetdevelopment.diyha.serial.{SerialPortConfig, SerialPortHelper}
import com.typesafe.scalalogging.LazyLogging
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, JValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object DiyhaApp extends App with Config with CassandraClient with LazyLogging {

  override def main(args: Array[String]) = {

    logger.info("Started DiyhaApp...")

    def jsonCallback(value: JValue) = {
      implicit val formats = DefaultFormats

      val dayFormat = new SimpleDateFormat("MM-dd-yyyy") // 05-04-2016

      logger.debug(Try {
        "Received JSON: " + compact(render(value))
      }.getOrElse {
        "Unable to render the JSON's JValue..."
      })

      val session = getSession()

      val now = new Date()

      val data = SensorData(
        (value \ "nodeId").extract[String],
        dayFormat.format(now),
        now,
        (value \ "airTemp").extract[Double],
        (value \ "humidity").extract[Double],
        (value \ "heatIndex").extract[Double],
        (value \ "waterTemp").extract[Double])

      val insertStmt: BoundStatement = new BoundStatement(session.prepare(
        "insert into diyhatest.station_data_by_day (station_id, date, log_time, temp, humidity, heat_index, water_temp) " +
          "values (?, ?, ?, ?, ?, ?, ?)")).bind(data.station_id, data.date, data.timestamp, data.temp, data.humidity, data.heat_index, data.water_temp)

      session.execute(insertStmt)

      session.close()
    }


    val f = Future {
      val spConfig = SerialPortConfig(
        getConfigString("comPort.portName", "/dev/ttyAMA0"),
        getConfigInt("comPort.baudRate", 115200),
        getConfigInt("comPort.dataBits", 8),
        getConfigInt("comPort.stopBits", 1),
        getConfigInt("comPort.parity", 0)
      )

      val spHelper = new SerialPortHelper(spConfig, jsonCallback)

      while (true) {
        Thread.sleep(250)
      }
    }

    f.onComplete {
      case Success(value) => logger.debug("Successfully shut down serial port thread")
      case Failure(e) => logger.debug("Error while reading from serial port")
    }

    while (true) {
      Thread.sleep(500)
    }

    System.exit(0)
  }

}

case class SensorData(station_id: String, date: String, timestamp: Date, temp: java.lang.Double,
                      humidity: java.lang.Double, heat_index: java.lang.Double, water_temp: java.lang.Double)