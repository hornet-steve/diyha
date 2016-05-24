package cassandra

import com.datastax.driver.core.{Cluster, Session}
import play.api.Play.current

/**
  * Created by steve on 5/20/16.
  */
trait CassandraHelper {

  private val config = current.configuration

  private val host = config.getString("cassandra.host").getOrElse("localhost")
  private val port = config.getInt("cassandra.port").getOrElse(9042)
  private val cUsername = config.getString("cassandra.user").getOrElse("diyhatest")
  private val cPw = config.getString("cassandra.pw").getOrElse("password")
  private val keyspace = config.getString("cassandra.keyspace").getOrElse("diyhatest")

  private[this] var session: Session = null

  private val cluster = Cluster.builder()
    .addContactPoint(host)
    .withPort(port)
    .withCredentials(cUsername.trim, cPw.trim).build

  def getSession() = {
    if (session != null && session.isClosed) {
      session = null
    }
    if (session == null) {
      session = cluster.connect(keyspace)
    }
    session
  }
}
