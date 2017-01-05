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

import akka.actor.{ Actor, ActorRef, Props }

object PublishSubscribeMediator {
  final val Name = "publish-subscribe-mediator"

  final case class Publish(topic: String, message: Any)
  final case class Published(publish: Publish)

  final case class Subscribe(topic: String, subscriber: ActorRef)
  final case class Subscribed(subscribe: Subscribe)
  final case class AlreadySubscribed(subscribe: Subscribe)

  def props = Props(new PublishSubscribeMediator)
}

class PublishSubscribeMediator extends Actor {
  import PublishSubscribeMediator._

  private var subscribers = Map.empty[String, Set[ActorRef]].withDefaultValue(Set.empty)

  override def receive: Receive = {
    case publish @ Publish(topic, message) =>
      subscribers(topic).foreach(_ ! message)
      sender() ! Published(publish)

    case subscribe @ Subscribe(topic, subscriber) if subscribers(topic).contains(subscriber) =>
      sender() ! AlreadySubscribed(subscribe)

    case subscribe @ Subscribe(topic, subscriber) =>
      subscribers += topic -> (subscribers(topic) + subscriber)
      sender() ! Subscribed(subscribe)
  }
}
