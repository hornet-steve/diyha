package com.hornetdevelopment.diyha

import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.{TimeZone, Date}

import com.datastax.driver.core.BoundStatement
import com.hornetdevelopment.diyha.cassandra.CassandraClient
import com.hornetdevelopment.diyha.config.Config
import com.hornetdevelopment.diyha.serial.{SerialPortConfig, SerialPortHelper}
import com.typesafe.scalalogging.LazyLogging
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, JValue}

import scala.collection.convert.Wrappers.JEnumerationWrapper
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object DiyhaApp extends App with Config with CassandraClient with LazyLogging {

  lazy val dataInsertStmt: BoundStatement = new BoundStatement(getSession().prepare(
    "insert into diyhatest.station_data_by_day (station_id, date, log_time, temp, humidity, heat_index, water_temp) " +
      "values (?, ?, ?, ?, ?, ?, ?)"))
  lazy val ipInsertStmt: BoundStatement = new BoundStatement(getSession().prepare(
    "insert into diyhatest.station_coordinator_ip (ip_address, changed_on) values (?, ?)"))

  lazy val dayFormat = {
    val fmt = new SimpleDateFormat("MM-dd-yyyy") // 05-04-2016
    fmt.setTimeZone(TimeZone.getTimeZone("CST6CDT"))
    fmt
  }

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

    logger.debug(Try {
      "Received JSON: " + compact(render(value))
    }.getOrElse {
      "Unable to render the JSON's JValue..."
    })

    def persistSensorData(data: SensorData): Try[Unit] = {
      Try {
        val session = getSession()
        if (data != null) {
          dataInsertStmt.bind(data.station_id, data.date, data.timestamp, data.temp.getOrElse(null), data.humidity.getOrElse(null),
            data.heat_index.getOrElse(null), data.water_temp.getOrElse(null))
          session.execute(dataInsertStmt)
        }
        session.close()
      }
    }

    val now = new Date()

    Try[SensorData] {
      SensorData(
        (value \ "nodeId").extract[String],
        dayFormat.format(now),
        now,
        (value \ "airTemp").extractOpt[java.lang.Double],
        (value \ "humidity").extractOpt[java.lang.Double],
        (value \ "heatIndex").extractOpt[java.lang.Double],
        (value \ "waterTemp").extractOpt[java.lang.Double]
      )
    } match {
      case Failure(e) => {
        logger.error(s"Unable to extract SensorData values from json: ${value}", e)
      }
      case Success(sd) => {
        persistSensorData(sd) match {
          case Success(nothing) => {
            logger.debug("Persisted data to Cassandra")
          }
          case Failure(e) => {
            // add to queue for the next try when connectivity is restored
            logger.error(s"Unable to write data ${compact(render(value))} to Cassandra. Storing locally.", e)
          }
        }
      }
    }
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

// why java.lang.Double? Because the datastax java driver had some issues mapping from Scala and the converter wasn't handling it...
case class SensorData(station_id: String, date: String, timestamp: Date, temp: Option[java.lang.Double],
                      humidity: Option[java.lang.Double], heat_index: Option[java.lang.Double], water_temp: Option[java.lang.Double])