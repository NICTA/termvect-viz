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

package org.t3as.termvect

import java.io.StringWriter

import org.apache.lucene.document.{Field, StringField}
import org.apache.lucene.index.{DirectoryReader, TermsEnum}
import org.apache.lucene.store.RAMDirectory
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import LuceneUtil.{TermPred, docFreqIter, index, mkDoc}
import Main.{contentFieldName, pathAction, pathFieldName, termAction, textFieldWithTermVectors}

class TestTermVect extends FlatSpec with Matchers {  
  val log = LoggerFactory.getLogger(getClass)

  "TermVect" should "make term vectors" in {
    val dir = new RAMDirectory
    index(dir, docs)

    val termWriter = new StringWriter
    val pathWriter = new StringWriter
    val indexReader = DirectoryReader.open(dir)
    indexReader.maxDoc should be (3)
    
    val termPred: TermPred = { (term: String, ti: TermsEnum) => ti.docFreq >= 2 }
    
    // calculate tf.idf = tf/df to keep it simple
    val idfs = docFreqIter(indexReader, contentFieldName, termPred).map { case(term, df) => (term, 1.0d/df) }.toSeq
    val tfCalc = (tf: Int, _: Int) => tf.toDouble // Wikipedia 1
    
    val actions = Seq(
        termAction(indexReader, tfCalc, idfs, termWriter),
        pathAction(indexReader, pathWriter))
    for {
      id <- 0 until indexReader.maxDoc // for each doc in the index
      action <- actions
    } action(id)
    
    // log.debug(s"terms = ${termWriter.toString}, paths = ${pathWriter.toString}")
    
    termWriter.toString should be("""3 3
0.0 0.3333333333333333 0.5
0.5 0.3333333333333333 0.5
0.5 0.3333333333333333 0.0
""") // trailing \n required
    
    pathWriter.toString should be("""doc1
doc2
doc3
""") // trailing \n required
  }

  def docs = {
    Stream(
      mkDoc(Seq(
        new Field(pathFieldName, "doc1", StringField.TYPE_STORED),
        new Field(contentFieldName, "fred sally mary", textFieldWithTermVectors))),
      mkDoc(Seq(
        new Field(pathFieldName, "doc2", StringField.TYPE_STORED),
        new Field(contentFieldName, "sally mary harry", textFieldWithTermVectors))),
      mkDoc(Seq(
        new Field(pathFieldName, "doc3", StringField.TYPE_STORED),
        new Field(contentFieldName, "mary harry higgs", textFieldWithTermVectors))))
    
    // term freqs for terms occurring in more than one doc are:
    //       harry mary sally
    // doc1    0     1    1
    // doc2    1     1    1
    // doc3    1     1    0
    // docFreq 2     3    2
    
  }
}

