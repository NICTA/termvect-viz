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

import java.io.{BufferedWriter, File, FileOutputStream, OutputStreamWriter, Writer}

import scala.collection.immutable.Stream.consWrapper

object IOUtil {

  /** Recurse directory tree.
    * http://stackoverflow.com/questions/2637643/how-do-i-list-all-files-in-a-subdirectory-in-scala
    */
  def dirTree(root: File, skipHidden: Boolean = true): Stream[File] =
    if (!root.exists || (skipHidden && root.isHidden)) Stream.empty
    else root #:: (
      root.listFiles match {
        case null => Stream.empty
        case files => files.toStream.flatMap(dirTree(_, skipHidden))
      })

  /** Get a writer for the file or NullWriter */
  def toWriter(o: Option[File]) = {
    o map (f => new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"))) getOrElse (NullWriter)
  }
  
  /** Writer to /dev/null */
  object NullWriter extends Writer {
    @Override def write(c: Array[Char], str: Int, end: Int) = {}
    @Override def flush = {}
    @Override def close = {}
  }

}