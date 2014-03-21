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

import java.io.{ File, Writer }
import java.nio.charset.CodingErrorAction
import java.util.regex.Pattern

import scala.Option.option2Iterable
import scala.collection.immutable.Stream.canBuildFrom
import scala.io.{ Codec, Source }

import org.apache.lucene.document.{ Field, FieldType, StringField, TextField }
import org.apache.lucene.index.{ DirectoryReader, IndexReader, TermsEnum }
import org.apache.lucene.store.FSDirectory
import org.slf4j.LoggerFactory

import IOUtil.{ dirTree, toWriter }
import LuceneUtil.{ TermPred, docFreqIter, index, mkDoc, termFreqIter }
import resource.managed

object Main {
  val log = LoggerFactory.getLogger(getClass)

  implicit val codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.IGNORE)
  codec.onUnmappableCharacter(CodingErrorAction.IGNORE)

  val pathFieldName = "path"
  val contentFieldName = "content"

  case class MyConfig(
    docDir: Option[File] = None,
    docSuffix: String = "",
    docList: Option[File] = None,
    indexDir: File = new File("index"),
    minDocFreq: Int = 2,
    maxDocFreq: Int = Int.MaxValue,
    excludeTerm: Option[String] = None,
    tfCalc: Int = 0,
    idfCalc: Int = 0,
    terms: Option[File] = None,
    paths: Option[File] = None,
    corpus: Option[File] = None)

  val parser = new scopt.OptionParser[MyConfig]("termvec") {
    head("termvect", "0.x")
    val defValue = MyConfig()

    opt[File]('d', "docDir") action { (x, c) =>
      c.copy(docDir = Some(x))
    } text ("directory holding text files to index")

    opt[String]('s', "docSuffix") action { (x, c) =>
      c.copy(docSuffix = x)
    } text (s"suffix of files to be indexed, default '${defValue.docSuffix}'")

    opt[File]('l', "docList") action { (x, c) =>
      c.copy(docList = Some(x))
    } text ("file containing paths of files to be indexed (alternative to docDir and docSuffix)")

    opt[File]('i', "indexDir") action { (x, c) =>
      c.copy(indexDir = x)
    } text (s"directory in which a Lucene index is created, default ${defValue.indexDir.getPath} (need not pre-exist)")

    opt[Int]('n', "minDocFreq") action { (x, c) =>
      c.copy(minDocFreq = x)
    } text (s"term vectors include only terms that occur in at least this number of docs, default ${defValue.minDocFreq}")

    opt[Int]('x', "maxDocFreq") action { (x, c) =>
      c.copy(maxDocFreq = x)
    } text (s"term vectors include only terms that occur in at most this number of docs, default ${defValue.maxDocFreq}")

    opt[String]('e', "excludeTerm") action { (x, c) =>
      c.copy(excludeTerm = Some(x))
    } text ("term vectors exclude terms matching this regex")

    opt[Int]("tfCalc") action { (x, c) =>
      c.copy(tfCalc = x)
    } text (s"tf calculation, 0 Lucene sqrt, 1 Wikipedia raw, 2 Wikipedia log, 3 Wikipedia prevent bias towards long docs; default ${defValue.tfCalc}")

    opt[Int]("idfCalc") action { (x, c) =>
      c.copy(idfCalc = x)
    } text (s"idf calculation, 0 Lucene log + 1, 1 Wikipedia log, 2 non-log version of 1, 3 none; default ${defValue.idfCalc}")

    opt[File]('t', "terms") action { (x, c) =>
      c.copy(terms = Some(x))
    } text ("file to write term vectors to (in index order)")

    opt[File]('p', "paths") action { (x, c) =>
      c.copy(paths = Some(x))
    } text ("file to write write doc paths to (in same order as term vectors)")

    opt[File]('c', "corpus") action { (x, c) =>
      c.copy(corpus = Some(x))
    } text ("file to write corpus term doc-freqs and term-freqs to")

    help("help") text ("prints this usage text")
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, MyConfig()) foreach doit
  }

  private def doit(c: MyConfig) = {
    import IOUtil.{ toWriter, dirTree }

    // if docDir or docList specified, index files, create Lucene index
    if (c.docDir.isDefined || c.docList.isDefined) {
      val t = new Timer

      val docs = c.docList.map(Source.fromFile(_).getLines.toStream.map(new File(_))) // Files from docList file
        .orElse(c.docDir map (dirTree(_) filter (f => f.isFile && f.getName.endsWith(c.docSuffix)))) // or from under docDir
        .getOrElse(Stream()).map(f => mkDoc(mkFields(f)))

      index(FSDirectory.open(c.indexDir), docs)

      log.info(s"Indexing complete in ${t.elapsedSecs} secs.")
    }

    if (c.terms.isDefined || c.paths.isDefined || c.corpus.isDefined) {
      // perform actions for each doc in the index
      val t = new Timer

      // Filtering out terms with lowest and highest doc freqs as well as numbers, as these are not useful for distinguishing subject.
      // Filtering out the terms with the lowest doc freqs greatly reduces the number of terms.
      val optRe = c.excludeTerm map (re => Pattern.compile(re))
      val termPred: TermPred = { (term: String, ti: TermsEnum) => ti.docFreq >= c.minDocFreq && ti.docFreq <= c.maxDocFreq && optRe.map(re => !re.matcher(term).matches()).getOrElse(true) }

      val tfCalc = c.tfCalc match {
        case 1 => {
          log.debug("tfCalc: Wikipedia 1, raw tf")
          (tf: Int, _: Int) => tf.toDouble
        }
        case 2 => {
          log.debug("tfCalc: Wikipedia 3, log(tf + 1)")
          (tf: Int, _: Int) => Math.log(tf.toDouble + 1.0d)
        }
        case 3 => {
          log.debug("tfCalc: Wikipedia 4, 0.5 + 0.5 * tf / max(tf)")
          (tf: Int, maxTf: Int) => 0.5d + 0.5d * tf.toDouble / maxTf.toDouble
        }
        case _ => {
          log.debug("tfCalc: Lucene, sqrt(tf)")
          (tf: Int, _: Int) => Math.sqrt(tf.toDouble)
        }
      }

      for {
        indexReader <- managed(DirectoryReader.open(FSDirectory.open(c.indexDir)))
        termWriter <- managed(toWriter(c.terms))
        pathWriter <- managed(toWriter(c.paths))
        corpusTfWriter <- managed(toWriter(c.corpus))
        // TODO: tidy way of combining two functions
        pred = c.corpus map { _ =>
          log.info("Will corpus term docFreqs and totalTermFreqs.")
          
          (term: String, ti: TermsEnum) => {
            corpusTfWriter.write(s"${term} ${ti.docFreq} ${ti.totalTermFreq}\n")
            termPred(term, ti)
          }
        } getOrElse termPred
        numDocs = indexReader.numDocs.toDouble
        idfCalc = c.idfCalc match {
          case 1 => (df: Int) => Math.log(numDocs / df.toDouble) // Wikipedia
          case 2 => (df: Int) => numDocs / df.toDouble // non-log version of Wikipedia
          case 3 => (_: Int) => 1.0d // None
          case _ => (df: Int) => 1.0d + Math.log(numDocs / (df.toDouble + 1.0d)) // Lucene
        }
        idfs = docFreqIter(indexReader, contentFieldName, pred).map { case (term, df) => (term, idfCalc(df)) }.toSeq
        actions = Seq(
          c.terms.map(_ => termAction(indexReader, tfCalc, idfs, termWriter)),
          c.paths.map(_ => pathAction(indexReader, pathWriter))).flatten
        id <- 0 until indexReader.maxDoc // for each doc in the index
        action <- actions
      } action(id)

      log.info(s"Output complete in ${t.elapsedSecs} secs.")
    }
  }

  val textFieldWithTermVectors = {
    val f = new FieldType(TextField.TYPE_NOT_STORED)
    f.setStoreTermVectors(true)
    f.freeze
    f
  }

  def mkFields(f: File) = {
    Seq(
      new Field(pathFieldName, f.getPath, StringField.TYPE_STORED),
      new Field(contentFieldName, Source.fromFile(f).mkString, textFieldWithTermVectors))
  }

  /** Write document paths to a file in index order */
  def pathAction(indexReader: IndexReader, w: Writer): Int => Unit = {
    log.info("Will write doc paths.")

    { id: Int => LuceneUtil.docFieldValue(indexReader, id, pathFieldName) map (w.append(_).append("\n")) }
  }

  /** Write tf-idf term vectors to a file in index order */
  def termAction(indexReader: IndexReader, tfCalc: (Int, Int) => Double, idfs: Seq[(String, Double)], termWriter: Writer): Int => Unit = {
    log.info("Will write term vectors.")

    { id: Int =>
      val terms = indexReader.getTermVector(id, contentFieldName)
      if (terms != null) {
        val termVect = tfIdfVector(idfs, termFreqIter(terms).toMap, tfCalc)
        if (id == 0) {
          termWriter.write(s"${indexReader.maxDoc} ${termVect.length}\n") // 1st line is dimensions of following term freq matrix
        }
        termWriter.write(termVect.mkString("", " ", "\n")) // term vector

      } else log.warn(s"No termVector for doc id = $id")
    }
  }

  /** For each term in corpusTermFreq, output its tf-idf for the current doc (as represented by docTermFreqs). */
  def tfIdfVector(idfs: Seq[(String, Double)], termFreqs: Map[String, Int], tfCalc: (Int, Int) => Double) = {
    val maxTermFreq = termFreqs.values.max
    idfs.map { case (term, idf) => {
      val rawtf = termFreqs.getOrElse(term, 0)
      val tf = tfCalc(rawtf, maxTermFreq)
      // if (rawtf > 0) log.debug(s"tfCalc($rawtf, $maxTermFreq) = $tf for term $term, idf $idf")
      tf * idf
    } }
  }

}
