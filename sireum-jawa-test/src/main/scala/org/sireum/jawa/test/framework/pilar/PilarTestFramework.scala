package org.sireum.jawa.test.framework.pilar

import org.sireum.jawa.test.framework.TestFramework
import org.sireum.util.FileResourceUri
import org.sireum.jawa.MessageCenter._
import java.io.File
import java.net.URI
import org.sireum.pilar.parser.Parser
import org.sireum.pilar.ast.PilarAstNode
import org.sireum.pilar.ast.Model
import org.sireum.jawa.Transform
import org.sireum.jawa.symbolResolver.JawaSymbolTableBuilder
import org.sireum.jawa.symbolResolver.JawaSymbolTable
import org.sireum.jawa.JawaResolver
import org.sireum.jawa.alir.JawaAlirInfoProvider
import org.sireum.jawa.Center

class PilarTestFramework extends TestFramework {
	def Analyzing : this.type = this

  def title(s : String) : this.type = {
    _title = caseString + s
    this
  }

  def file(fileRes : FileResourceUri) =
    InterProceduralConfiguration(title, fileRes)
/**
 * does inter procedural analysis of an app
 * @param src is the uri of the apk file
 */
  case class InterProceduralConfiguration //
  (title : String,
   srcRes : FileResourceUri) {

    test(title) {
    	msg_critical("####" + title + "#####")
    	
    	val pilarFileUri = srcRes
    	val reporter = new Parser.StringErrorReporter
	    val modelOpt = Parser.parse[Model](Right(pilarFileUri), reporter, false)
	    if(modelOpt.isDefined){
	      msg_critical("Parsing OK!")
	      val st = JawaSymbolTableBuilder.apply(List(modelOpt.get), { _ : Unit => new JawaSymbolTable }, false)
	    } else {
	      err_msg_critical(reporter.errorAsString)
	    }
    	msg_critical("************************************\n")
    }
  }

  protected var _title : String = null
  protected var num = 0
  protected def title() = if (_title == null) {
    num += 1
    "Analysis #" + num
  } else _title
}