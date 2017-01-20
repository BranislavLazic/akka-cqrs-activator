/*
 * Copyright 2017 Branislav Lazic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.akkacqrs

import com.typesafe.config.{ Config, ConfigFactory }

import scala.concurrent.duration.{ FiniteDuration, NANOSECONDS }

object Settings {

  val config: Config = ConfigFactory.load()

  object CassandraDb {
    val contactPoints: Array[String] =
      config.getStringList("cassandra.read.contact-points").toArray.map(i => i.toString)
    val port: Int                           = config.getInt("cassandra.read.port")
    val keyspace: String                    = config.getString("cassandra.read.keyspace")
    val keyspaceReplicationStrategy: String = config.getString("cassandra.read.keyspace-replication.class")
    val keyspaceReplicationFactor: Int      = config.getInt("cassandra.read.keyspace-replication.replication-factor")
  }

  object Http {
    val host: String         = config.getString("http.host")
    val port: Int            = config.getInt("http.port")
    val eventBufferSize: Int = config.getInt("http.event-buffer-size")
    val requestTimeout: FiniteDuration =
      FiniteDuration(config.getDuration("http.request-timeout").toNanos, NANOSECONDS)
    val heartbeatInterval: FiniteDuration =
      FiniteDuration(config.getDuration("http.heartbeat-interval").toNanos, NANOSECONDS)
  }

}
