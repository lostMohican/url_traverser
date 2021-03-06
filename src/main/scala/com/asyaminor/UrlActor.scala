package com.asyaminor

import akka.actor.{Props, ActorLogging, Actor}
import com.asyaminor.MediatorActor._

import scalaj.http.Http

class UrlActor extends Actor with ActorLogging {

  type MeasureResult = (String, Long)
  val TRIAL_NUMBER = 20

  def getContent(url: String): String = {
    val fullUrl = url
    log.info("full url will be fetched: " + fullUrl)
    Http(fullUrl).asString.body
  }

  def measureHost(url: String): MeasureResult = {
    //first just get the response and calculate
    //next do 20 times and average
    //next take a look at warmup algorithms

    val avgFinal = (1 to TRIAL_NUMBER).foldLeft(0L)((avg, index) => {
      val result = time {
        val response = Http(url).asString
        response.body
      }

      accAvg(avg, index, result._2)
    })

    val result = time{
      val response = Http(url).asString
      log.debug(s"body fetched ${response.statusLine}")
      response.headers.foreach(header => log.debug(s"header: ${header._1} -> values: ${header._2}"))
      Http(url).asString.body
    }

    (result._1, avgFinal)
  }

  def receive = {
    case UrlMessage(url) =>

      val content = getContent(url)

      sender() ! HtmlResponse(content, url)

    case PerformanceMsg(url) =>

      val (body, time) = measureHost(url)

      sender() ! PerformanceResponse(body, url, time)

    case ComparisonMsg(url, avg) =>
      val (body, time) = measureHost(url)

      sender() ! ComparisonRespMsg(url, time, avg)
  }

}

object UrlActor {
  val props = Props[UrlActor]
  case object Ack
}

