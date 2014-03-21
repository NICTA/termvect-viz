/*
    Copyright 2013 NICTA
    
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

import org.slf4j.LoggerFactory
import org.scalatest.FlatSpec
import org.scalatest.Matchers

import Main._
import Math.sqrt

class TestClustEval extends FlatSpec with Matchers {  
  val log = LoggerFactory.getLogger(getClass)

  val p1 = Point(Seq(1, 2), "label")
  val p2 = Point(Seq(3, 5), "label")
  val p3 = Point(Seq(6, 9), "label")
  val p4 = Point(Seq(10, 14), "label")
  val points = IndexedSeq(p1, p2, p3, p4) 

  def sqr(x: Double) = x * x

  "avg" should "average Doubles and Ints" in {
    val x = avg(Seq((3.0, 9), (21.0, 3), (12.0, 0)).iterator)
    x should be (3.0)
  }
  
  "dist" should "produce distance" in {
    for {
      a <- points
      b <- points
    } a.dist(b) should be (sqrt( sqr(a.x(0) - b.x(0)) + sqr(a.x(1) - b.x(1)) ))
  }

  "sumDist" should "produce sum of distances" in {
    val x = sumDist(
      Seq(p1, p2),
      Seq(p3, p4)
    )
    x should be (p1.dist(p3) + p1.dist(p4) + p2.dist(p3) + p2.dist(p4))
  }
  
  "avgDistWithinClass" should "produce average distance" in {
    val m = Map("label1" -> IndexedSeq(p1, p2, p3), "label2" -> IndexedSeq(p1, p2, p3))
    avgDistWithinClass(m) should be ((p1.dist(p2) + p1.dist(p3) + p2.dist(p3)) / 3.0d)
  }
  
  "avgDistBetweenClass" should "produce average distance" in {
    val m = Map("label1" -> IndexedSeq(p1, p2), "label2" -> IndexedSeq(p3, p4))
    avgDistBetweenClass(m) should be ((p1.dist(p3) + p1.dist(p4) + p2.dist(p3) + p2.dist(p4)) / 4.0d)
  }
}

