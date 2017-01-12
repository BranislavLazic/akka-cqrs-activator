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

package org

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.google.common.util.concurrent.{ FutureCallback, Futures, ListenableFuture }
import io.circe.Encoder

import scala.concurrent.{ Future, Promise }
import scala.reflect.{ ClassTag, classTag }

package object akkacqrs {

  type Traversable[+A] = scala.collection.immutable.Traversable[A]
  type Iterable[+A]    = scala.collection.immutable.Iterable[A]
  type Seq[+A]         = scala.collection.immutable.Seq[A]
  type IndexedSeq[+A]  = scala.collection.immutable.IndexedSeq[A]

  implicit class RichResultSetFuture[ResultSet](rsf: ListenableFuture[ResultSet]) {
    def toFuture: Future[ResultSet] = {
      val promise = Promise[ResultSet]()
      Futures.addCallback(rsf, new FutureCallback[ResultSet] {
        def onFailure(t: Throwable): Unit = promise.failure(t)

        def onSuccess(result: ResultSet): Unit = promise.success(result)
      })
      promise.future
    }
  }

  implicit class DateTimeConverter[LocalDate](date: String) {
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    def toLocalDate: java.time.LocalDate     = LocalDate.parse(date, dateTimeFormatter)
  }

  def className[A: ClassTag]: String = classTag[A].runtimeClass.getName

  implicit val localDateEncoder: Encoder[LocalDate] = Encoder.encodeString.contramap[LocalDate](_.toString)
}
