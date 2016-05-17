package com.hornetdevelopment.diyha

import java.util.{Calendar, Date}

import com.datastax.driver.core.BoundStatement
import com.hornetdevelopment.diyha.cassandra.CassandraClient
import com.hornetdevelopment.diyha.config.Config
import com.hornetdevelopment.diyha.serial.{SerialPortConfig, SerialPortHelper}
import org.json4s.JValue

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object DiyhaApp extends App with Config with CassandraClient {

  override def main(args: Array[String]) = {

    def jsonCallback(value: JValue) = {
      val session = getSession()
//      val stmt: BoundStatement = new BoundStatement(session.prepare(s"select * from diyhatest.station_data_by_day where station_id = ? AND date = ?"))
//
//      val rs = session.execute(stmt.bind("TestSensor", "2016-05-11"))
//
//      for (row <- rs) {
//        println(s"Before: ${row.toString}")
//      }
//
//      val data = SensorData("TestSensor", "2016-05-11", 54.34, 33.33, 44.4, 35.13)
//
//      val insertStmt: BoundStatement = new BoundStatement(session.prepare(
//        "insert into diyhatest.station_data_by_day (station_id, date, log_time, temp, humidity, heat_index, water_temp) " +
//          "values (?, ?, ?, ?, ?, ?, ?)")).bind(data.station_id, data.date, new Date(), data.temp, data.humidity, data.heat_index, data.water_temp)
//
//
//      session.execute(insertStmt)
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

      var start = System.currentTimeMillis()
      val delay = 1000 * 60 * 5

      while (true) {
        if (System.currentTimeMillis() - start > delay) {
          //spHelper.sp.writeString(Calendar.getInstance().getTime + "\r\n")
          start = System.currentTimeMillis()
        }
      }
    }

    f.onComplete {
      case Success(value) => println("Successfully shut down serial port thread")
      case Failure(e) => println("Error while reading from serial port")
    }

    while (true) {
      Thread.sleep(50)
    }

    System.exit(0)
  }

}

case class SensorData(station_id: String, date: String, temp: java.lang.Double, humidity: java.lang.Double, heat_index: java.lang.Double, water_temp: java.lang.Double)