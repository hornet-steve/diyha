package controllers

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import cassandra.CassandraHelper
import com.datastax.driver.core.BoundStatement
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json._
import play.api.mvc._

import scala.util.{Failure, Success, Try}

class Application extends Controller with CassandraHelper with LazyLogging {

  val getTodaysDataStmt = new BoundStatement(getSession().prepare(s"select * from station_data_by_day where station_id = ? and date = ? order by log_time desc limit 1"))

  lazy val dayFormat = {
    val fmt = new SimpleDateFormat("MM-dd-yyyy") // 05-04-2016
    fmt.setTimeZone(TimeZone.getTimeZone("CST6CDT"))
    fmt
  }

  lazy val lastUpdatedFormat = {
    val fmt = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a zzz")
    fmt.setTimeZone(TimeZone.getTimeZone("CST6CDT"))
    fmt
  }

  val DEFAULT_STATION = "LakeSensor"

  def default() = {
    index(DEFAULT_STATION)
  }

  def index(stationId: String) = Action {
    Ok(views.html.index(stationId))
  }

  def getTempDataDefault = {
    getTempData(DEFAULT_STATION)
  }

  def getTempData(stationId: String) = Action {

    val today = new Date()

    val json: JsValue = Try {
      logger.debug(s"Executed cassandra query for stationId: ${stationId}")
      getSession().execute(getTodaysDataStmt.bind(stationId, dayFormat.format(today)))
    } match {
      case Success(rs) => {
        logger.debug(s"Successful query for stationId: ${stationId}")
        val currentDataRow = rs.one()

        if (currentDataRow != null) {
          Json.obj(
            "airTemp" -> Json.arr(Json.arr("Label", "Value"), Json.arr("", currentDataRow.getDouble("temp"))),
            "waterTemp" -> Json.arr(Json.arr("Label", "Value"), Json.arr("", currentDataRow.getDouble("water_temp"))),
            "humidity" -> Json.arr(Json.arr("Label", "Value"), Json.arr("", currentDataRow.getDouble("humidity"))),
            "heatIndex" -> Json.arr(Json.arr("Label", "Value"), Json.arr("", currentDataRow.getDouble("heat_index"))),
            "lastUpdated" -> lastUpdatedFormat.format(currentDataRow.getTimestamp("log_time"))
          )
        } else {
          getEmptyJson
        }
      }
      case Failure(e) => {
        logger.error(s"Error querying Cassandra for stationId: ${stationId}, sending default JSON.", e)
        getEmptyJson
      }
    }

    Ok(json)
  }

  def getEmptyJson = {
    Json.obj(
      "airTemp" -> Json.arr(Json.arr("Label", "Value"), Json.arr("Current", 0)),
      "waterTemp" -> Json.arr(Json.arr("Label", "Value"), Json.arr("Current", 0)),
      "humidity" -> Json.arr(Json.arr("Label", "Value"), Json.arr("Current", 0)),
      "heatIndex" -> Json.arr(Json.arr("Label", "Value"), Json.arr("Current", 0)),
      "lastUpdated" -> lastUpdatedFormat.format(new Date())
    )
  }
}