/*
Copyright (c) 2013-2014 Fengguo Wei & Sankardas Roy, Kansas State University.        
All rights reserved. This program and the accompanying materials      
are made available under the terms of the Eclipse Public License v1.0 
which accompanies this distribution, and is available at              
http://www.eclipse.org/legal/epl-v10.html                             
*/
package org.sireum.jawa.util

import org.sireum.util._
import java.net.URL
import java.io.File
import java.util.jar.JarFile
import java.net.URLDecoder
import java.io.FileReader
import java.io.LineNumberReader
import java.net.URI

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
object MyFileUtil {
  
  def readFileContent(fileResourceUri : FileResourceUri) : String = {
    val fr = new FileReader(new File(new URI(fileResourceUri)))
    try{
      val lnr = new LineNumberReader(fr)
	    var sb = new StringBuilder
	    var lineText = lnr.readLine
	    while (lineText != null) {
	      sb.append(lineText)
	      sb.append('\n')
	      lineText = lnr.readLine
	    }
      sb.toString
    } finally fr.close
  }
  
  def deleteDir(dir : File) : Boolean = {
    if (dir.isDirectory()) {
       val children = dir.list()
       for (i <- 0 to children.length - 1) {
      	 val success = deleteDir(new File(dir, children(i)));
          if (!success) {
             return false;
          }
       }
    }
    dir.delete();
  }
  
	/**
   * List directory contents for a resource folder. Not recursive.
   * This is basically a brute-force implementation.
   * Works for regular files and also JARs.
   * 
   * @author Greg Briggs
   * @param clazz Any java class that lives in the same place as the resources you want.
   * @param path Should end with "/", but not start with one.
   * @return Just the name of each member item, not the full paths.
   * @throws URISyntaxException 
   * @throws IOException 
   */
  def getResourceListing[C](clazz : Class[C], path : String, ext : String) : ISet[String] = {
      var dirURL = clazz.getResource(path)
      if (dirURL != null && dirURL.getProtocol().equals("file")) {
        /* A file path: easy enough */
        return new File(dirURL.toURI()).list().filter(_.endsWith(ext)).toSet
      } 

      if (dirURL == null) {
        /* 
         * In case of a jar file, we can't actually find a directory.
         * Have to assume the same jar as clazz.
         */
        val me = clazz.getName().replace(".", "/")+".class"
        dirURL = clazz.getClassLoader().getResource(me)
      }
      
      if (dirURL.getProtocol().equals("jar")) {
        /* A JAR path */
        val jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")) //strip out only the JAR file
        val jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))
        val entries = jar.entries() //gives ALL entries in jar
        var result = isetEmpty[String] //avoid duplicates in case it is a subdirectory
        while(entries.hasMoreElements()) {
          val name = entries.nextElement().getName()
          if (name.startsWith(path) && name.endsWith(ext)) { //filter according to the path
            var entry = name.substring(path.length())
            val checkSubdir = entry.indexOf("/")
            if (checkSubdir >= 0) {
              // if it is a subdirectory, we just return the directory name
              entry = entry.substring(0, checkSubdir)
            }
            result += entry
          }
        }
        return result
      } 
        
      throw new UnsupportedOperationException("Cannot list files for URL "+dirURL);
  }
}