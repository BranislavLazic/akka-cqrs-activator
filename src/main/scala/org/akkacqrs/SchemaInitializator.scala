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

import java.io.InputStream

import scala.io.Source
import scala.util.{ Failure, Success, Try }

object SchemaInitializator {

  final val StatementDelimiter = ";"

  def readCQLFile(file: String): Try[String] =
    if (!file.endsWith(".cql"))
      Failure(new IllegalArgumentException("File must have a .cql extension!"))
    else {
      val stream: InputStream = getClass.getResourceAsStream(file)
      Success(Source.fromInputStream(stream).getLines.foldLeft("")((l, r) => l.concat(r)))
    }

  def getStatements(schemaLiteral: String): Vector[String] =
    schemaLiteral.split(StatementDelimiter).toVector

  def apply(files: Vector[String]): Unit = {
    val session = CassandraConnector.openConnection()
    files.foreach { file =>
      readCQLFile(file) match {
        case Success(schemaLiteral) =>
          getStatements(schemaLiteral).foreach(session.execute)
          CassandraConnector.closeConnection(session)
        case Failure(_) =>
      }
    }
  }

}
