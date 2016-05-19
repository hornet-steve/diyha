package com.hornetdevelopment.diyha.serial

import jssc.SerialPort
import org.json4s.JsonAST.JValue

/**
  * Created by steve on 5/13/16.
  */
class SerialPortHelper(config: SerialPortConfig, callback: JValue => Unit) {
  val sp = new SerialPort(config.portName)
  sp.openPort()
  sp.setParams(config.baudRate, config.dataBits, config.stopBits, config.parity)
  sp.addEventListener(new SerialReader(sp, callback))
}

case class SerialPortConfig(portName: String, baudRate: Int, dataBits: Int, stopBits: Int, parity: Int)
