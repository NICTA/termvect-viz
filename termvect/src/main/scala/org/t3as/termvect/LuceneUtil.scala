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

package org.t3as.termvect

import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.document.{Document, Field}
import org.apache.lucene.index.{FieldInfo, IndexReader, IndexWriter, IndexWriterConfig, SlowCompositeReaderWrapper, StoredFieldVisitor}
import org.apache.lucene.index.{Terms, TermsEnum}
import org.apache.lucene.index.StoredFieldVisitor.Status.{NO, YES}
import org.apache.lucene.store.Directory
import org.apache.lucene.util.{Bits, Version}
import org.slf4j.LoggerFactory

import resource.managed

object LuceneUtil {
  val log = LoggerFactory.getLogger(getClass)
  val version = Version.LUCENE_46

  /** Analyzer using:
    * StandardTokenizer, StandardFilter, EnglishPossessiveFilter, LowerCaseFilter, StopFilter, PorterStemFilter.
    */
  val analyzer = new EnglishAnalyzer(version)

  def mkDoc(fields: Seq[Field]) = {
    fields.foldLeft(new Document) { (doc, f) =>
      doc.add(f)
      doc
    }
  }

  val indexWriterConfig = {
    val c = new IndexWriterConfig(version, analyzer)
    c.setOpenMode(IndexWriterConfig.OpenMode.CREATE)
    c
  }

  def index(dir: Directory, docs: Stream[Document]) = {
    for {
      w <- managed(new IndexWriter(dir, indexWriterConfig))
      d <- docs
    } w.addDocument(d)
  }
  
  class OneStringFieldVisitor(name: String) extends StoredFieldVisitor {
    import StoredFieldVisitor.Status._
    
    var value: Option[String] = None
    def needsField(i: FieldInfo) = if (i.name == name) YES else NO
    override def stringField(i: FieldInfo, v: String) = value = Some(v)
  }

  /** get a single value from a doc */
  def docFieldValue(indexReader: IndexReader, id: Int, name: String) = {
    val visitor = new OneStringFieldVisitor(name)
    indexReader.document(id, visitor)
    visitor.value
  }

  def termIter(terms: Terms) = {
    val ti = terms.iterator(null)
    Iterator.continually(ti.next).takeWhile(_ != null).map(br => (br.utf8ToString, ti))
  }
  
  def termFreqIter(terms: Terms) = for ((term, ti) <- termIter(terms)) yield (term, freq(ti))

  /** freq of current term */
  def freq(ti: TermsEnum) = {
    val di = ti.docs(liveDocs, null)
    // DocsEnum has an iterator like interface, but here there is always exactly 1 value
    // val docFreq = Iterator.continually(de.nextDoc).takeWhile(_ != DocIdSetIterator.NO_MORE_DOCS).map(id => (id, de.freq))
    di.nextDoc
    di.freq
  }
  val liveDocs = new Bits.MatchAllBits(1) // anything >= 1 will do

  type TermPred = (String, TermsEnum) => Boolean

  /** Document frequency.
    * For each term in the corpus that satisfies p, get the term and the number of docs it appears in.
    * Terms are returned in lexical order.
    */
  def docFreqIter(terms: Terms, p: TermPred): Iterator[(String, Int)] = {
    for {
      (term, ti) <- termIter(terms)
      if p(term, ti)
    } yield (term, ti.docFreq)
  }
  
  def docFreqIter(indexReader: IndexReader, field: String, p: TermPred): Iterator[(String, Int)] = {
    docFreqIter(SlowCompositeReaderWrapper.wrap(indexReader).terms(field), p)
  }

}