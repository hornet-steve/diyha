package com.hornetdevelopment.diyha

import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date

import com.datastax.driver.core.BoundStatement
import com.hornetdevelopment.diyha.cassandra.CassandraClient
import com.hornetdevelopment.diyha.config.Config
import com.hornetdevelopment.diyha.serial.{SerialPortConfig, SerialPortHelper}
import com.typesafe.scalalogging.LazyLogging
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, JValue}

import scala.collection.convert.Wrappers.JEnumerationWrapper
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object DiyhaApp extends App with Config with CassandraClient with LazyLogging {

  lazy val dataInsertStmt: BoundStatement = new BoundStatement(getSession().prepare(
    "insert into diyhatest.station_data_by_day (station_id, date, log_time, temp, humidity, heat_index, water_temp) " +
      "values (?, ?, ?, ?, ?, ?, ?)"))
  lazy val ipInsertStmt: BoundStatement = new BoundStatement(getSession().prepare(
    "insert into diyhatest.station_coordinator_ip (ip_address, changed_on) values (?, ?)"))

  def logLocalIp() = {
    Try {
      JEnumerationWrapper(NetworkInterface.getNetworkInterfaces).foreach { interface =>
        JEnumerationWrapper(interface.getInetAddresses).foreach { inetAddress =>
          val hostAddress = inetAddress.getHostAddress
          if (hostAddress.contains("192.168")) {
            logger.debug(s"Logging private ip: ${hostAddress}")
            ipInsertStmt.bind(hostAddress, new Date())
            getSession.execute(ipInsertStmt)
          } else {
            logger.debug(s"Ignoring ip: ${hostAddress}")
          }
        }
      }
    } match {
      case Failure(e) =>
        logger.error("Error inserting ip address", e)
      case _ =>
    }
  }

  def jsonCallback(value: JValue) = {
    logLocalIp()
    implicit val formats = DefaultFormats

    val dayFormat = new SimpleDateFormat("MM-dd-yyyy") // 05-04-2016

    logger.debug(Try {
      "Received JSON: " + compact(render(value))
    }.getOrElse {
      "Unable to render the JSON's JValue..."
    })

    val now = new Date()

    val data = Try {
      SensorData(
        (value \ "nodeId").extract[String],
        dayFormat.format(now),
        now,
        (value \ "airTemp").extract[Double],
        (value \ "humidity").extract[Double],
        (value \ "heatIndex").extract[Double],
        (value \ "waterTemp").extract[Double])
    }.getOrElse {
      logger.error(s"Unable to extract SensorData values from json: ${value}")
      null
    }

    val session = getSession()

    if (data != null) {
      dataInsertStmt.bind(data.station_id, data.date, data.timestamp, data.temp, data.humidity, data.heat_index, data.water_temp)
      session.execute(dataInsertStmt)
    }

    session.close()
  }

  override def main(args: Array[String]) = {
    logger.info("Started DiyhaApp...")
    logLocalIp()

    var running = true

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run(): Unit = running = false
    })

    val f = Future {
      val spConfig = SerialPortConfig(
        getConfigString("comPort.portName", "/dev/ttyAMA0"),
        getConfigInt("comPort.baudRate", 115200),
        getConfigInt("comPort.dataBits", 8),
        getConfigInt("comPort.stopBits", 1),
        getConfigInt("comPort.parity", 0)
      )

      val spHelper = new SerialPortHelper(spConfig, jsonCallback)

      while (running) {
        Thread.sleep(250)
      }

      "Shutdown Complete"
    }

    f.onComplete {
      case Success(value) => logger.debug(s"Successfully shut down serial port thread. Message: ${value}")
      case Failure(e) => logger.error("Error while reading from serial port", e)
    }
  }


}

case class SensorData(station_id: String, date: String, timestamp: Date, temp: java.lang.Double,
                      humidity: java.lang.Double, heat_index: java.lang.Double, water_temp: java.lang.Double)