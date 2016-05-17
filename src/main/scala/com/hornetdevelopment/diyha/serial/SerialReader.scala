package com.hornetdevelopment.diyha.serial

import com.hornetdevelopment.diyha.config.Config
import jssc.{SerialPort, SerialPortEvent, SerialPortEventListener}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.Try

/**
  * Created by steve on 5/13/16.
  */
class SerialReader(serialPort: SerialPort, callback: JValue => Unit) extends SerialPortEventListener with Config {

  val readBuffer = new StringBuilder()
  val retryCount = getConfigInt("comPort.jsonReadRetryCount", 4)
  var retries = 0

  override def serialEvent(serialPortEvent: SerialPortEvent): Unit = {
    serialPortEvent match {
      case e if e.isRXCHAR && e.getEventValue > 0 => {
        readBuffer.append(serialPort.readString(e.getEventValue))
        // todo - implement a much more elegant and fault tolerant protocol :P
        if (readBuffer.endsWith("\r\n")) {

          val json: Option[JValue] = Try {
            Some(parse(readBuffer.toString()))
          }.getOrElse(None)

          json match {
            case Some(value) => {
              callback(value)
              readBuffer.clear
            }
            case None => {
              retries += 1
              if (retries > retryCount) {
                println("Retry count exceeded, flushing buffer")
                retries = 0
                readBuffer.clear
              } else {
                println(s"Unable to parse JSON: ${readBuffer.toString}, retrying...")
              }
            }
            case _ => {
              println(s"Unknown data received: ${readBuffer.toString}")
              readBuffer.clear
            }
          }
        }
      }
      case _ =>
    }
  }
}
