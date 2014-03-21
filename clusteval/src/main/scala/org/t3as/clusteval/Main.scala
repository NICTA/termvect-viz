/*
    Copyright 2014 NICTA
    
    This file is part of t3as (Text Analysis As A Service).

    t3as is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    t3as is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with t3as.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.t3as.clusteval

import scala.io.Source
import org.slf4j.LoggerFactory
import scala.language.postfixOps

object Main {
  val log = LoggerFactory.getLogger(getClass)

  case class Point(x: Seq[Double], label: String) {

    def dist(p: Point) = {
      import Math.sqrt
      def sqr(x: Double) = x * x

      sqrt(x zip p.x map { case (a, b) => sqr(a - b) } sum)
    }
  }

  /** (average distance between pairs of documents in same class) / (average distance between pairs of documents in different classes)
    */
  def main(args: Array[String]): Unit = {
    for (fname <- args) {
      // Map: label -> Seq[Point]
      val m = Source.fromFile(fname, "UTF-8").getLines.map { line =>
        val arr = line.split("\\s+")
        Point(arr.toSeq.dropRight(1).map(_.toDouble), arr.last)
      }.toIndexedSeq.groupBy(_.label)
      log.debug(s"m = $m");
      println(s"$fname ${avgDistWithinClass(m) / avgDistBetweenClass(m)}")
    }
  }

  def avgDistWithinClass(m: Map[String, IndexedSeq[Point]]) = {
    val x = for ((l, p) <- m.iterator) yield {
      val y = (sumDist(p, p), p.size * (p.size - 1)) // -1 to avoid counting p.dist(q) where p = q
      log.debug(s"(sum, count) for label $l: $y")
      y
    }
    val y = avg(x)
    log.debug(s"avgDistWithinClass: $y")
    y
  }

  def avgDistBetweenClass(m: Map[String, IndexedSeq[Point]]) = {
    val x = for {
      (l1, p1) <- m.iterator
      (l2, p2) <- m.iterator
      if l1 < l2
    } yield {
      val y = (sumDist(p1, p2), p1.size * p2.size)
      log.debug(s"(sum, count) for labels $l1 & $l2: $y")
      y
    }
    val y = avg(x)
    log.debug(s"avgDistBetweenClass: $y")
    y
  }

  def avg(x: Iterator[(Double, Int)]) = {
    val (sum, count) = x.fold((0.0d, 0)) {
      case ((s1, c1), (s2, c2)) =>
        (s1 + s2, c1 + c2) // sum over labels
    }
    sum / count
  }

  def sumDist(a: Iterable[Point], b: Iterable[Point]) = {
    (for {
      x <- a.iterator
      y <- b.iterator
    } yield x.dist(y)).sum
  }

}