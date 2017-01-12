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

import akka.actor.Actor
import com.datastax.driver.core.Session

/**
  * Actor with ability to manage a lifecycle of Cassandra cluster and Cassandra session.
  */
trait CassandraActor extends Actor {

  val session: Session = CassandraConnector.openConnection()
  override def postStop(): Unit = {
    CassandraConnector.closeConnection(session)
  }
}
