package com.hornetdevelopment.diyha.cassandra

import com.datastax.driver.core.{Cluster, Session}
import com.hornetdevelopment.diyha.config.Config

/**
  * Created by steve on 5/11/16.
  */
trait CassandraClient extends Config {
  private val host = getConfigString("cassandra.host", "localhost")
  private val port = getConfigInt("cassandra.port", 9042)
  private val cUsername = getConfigString("cassandra.user", "diyhatest")
  private val cPw = getConfigString("cassandra.pw", "password")
  private val keyspace = getConfigString("cassandra.keyspace", "diyhatest")

  private[this] var session: Session = null

  private val cluster = Cluster.builder()
    .addContactPoint(host)
    .withPort(port)
    .withCredentials(cUsername.trim, cPw.trim).build

  def getSession() = {
    if (session != null && session.isClosed){
      session = null
    }
    if (session == null) {
      session = cluster.connect(keyspace)
    }
    session
  }

}
