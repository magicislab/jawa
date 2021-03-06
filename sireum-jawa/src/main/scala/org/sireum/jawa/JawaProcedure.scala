/*
Copyright (c) 2013-2014 Fengguo Wei & Sankardas Roy, Kansas State University.        
All rights reserved. This program and the accompanying materials      
are made available under the terms of the Eclipse Public License v1.0 
which accompanies this distribution, and is available at              
http://www.eclipse.org/legal/epl-v10.html                             
*/
package org.sireum.jawa

import org.sireum.jawa.util._
import org.sireum.jawa.MessageCenter._
import org.sireum.util._

/**
 * This class is an amandroid representation of a pilar procedure. It can belong to JawaRecord.
 * You can also construct it manually. 
 * 
 * 
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */
class JawaProcedure extends ResolveLevel with PropertyProvider {

  final val TITLE = "JawaProcedure"
	var DEBUG : Boolean = false
	
	/**
	 * short name of the procedure. e.g. equals
	 */
	
	protected var shortName : String = null
	
	/**
	 * full name of the procedure. e.g. java.lang.Object.equals
	 */
	
	protected var name : String = null
	
	/**
	 * signature of the procedure. e.g. Ljava/lang/Object;.equals:(Ljava/lang/Object;)Z
	 */
	
	protected var signature : String = null
	
	/**
	 * sub-signature of the procedure. e.g. equals:(Ljava/lang/Object;)Z
	 */
	
	protected var subSignature : String = null
	
	/**
	 * list of parameter types. e.g. List(java.lang.Object, java.lang.String)
	 */
	
	protected var paramTyps : List[Type] = List()
	
	/**
	 * list of parameter names
	 */
	
	protected var paramNames : List[String] = List()
	
	/**
	 * return type. e.g. boolean
	 */
	
	protected var returnTyp : Type = null
	
	/**
	 * declaring record of this procedure
	 */
	
	protected var declaringRecord : JawaRecord = null
	
	/**
   * the access flags integer represent for this procedure
   */
  
  protected var accessFlags : Int = 0
  
  /**
   * exceptions thrown by this procedure
   */
  
  protected var exceptions : Set[JawaRecord] = Set()
  
  /**
   * Data structure to store all information about a catch clause
   * LocUri should always looks like "L?[0-9a-f]+"
   */
  
  case class ExceptionHandler(exception : JawaRecord, fromTarget : String, toTarget : String, jumpTo : String){
	  def handleException(exc : JawaRecord, locUri : String) : Boolean = {
	    (exception == exc || exception.isChildOf(exc)) && withInScope(locUri)
	  }
	  def withInScope(locUri : String) : Boolean = {
	    getLocation(fromTarget) <= getLocation(locUri) && getLocation(locUri) <= getLocation(toTarget)
	  }
	  def getLocation(locUri : String) : Int = {
	    val loc = locUri.substring(locUri.lastIndexOf("L") + 1)
    	Integer.getInteger(loc, 16)
	  }
	}
  
  /**
   * exception handlers
   */
  
  protected var exceptionHandlers : IList[ExceptionHandler] = ilistEmpty
  
  /**
   * represents if the procedure is unknown.
   */
  
  protected var unknown : Boolean = false
  
  /**
   * hold the body symbol table of this procedure
   */
  
  protected var procBody : ProcedureBody = null
  
  /**
   * supply property
   */
  val propertyMap = mlinkedMapEmpty[Property.Key, Any]
  
  /**
	 * is it declared in some JawaRecord?
	 */
	
	def isDeclared : Boolean = declaringRecord != null
  
  /**
   * return hash code to provide structural equality
   */
  
  def strEquHashCode : Int = this.returnTyp.hashCode() * 101 + this.accessFlags * 17 + this.name.hashCode()
  
  /**
   * when you construct an amandroid procedure instance, call this init function first
   */
  
  def init(name : String, sig : String, paramTyps : List[Type], returnTyp : Type, accessFlags : Int, thrownExceptions : List[JawaRecord]) : JawaProcedure = {
	  setName(name)
	  setSignature(sig)
	  this.paramTyps ++= paramTyps
	  this.returnTyp = returnTyp
	  this.accessFlags = accessFlags
	  
	  if(exceptions.isEmpty || !thrownExceptions.isEmpty){
	    exceptions ++= thrownExceptions
	  }
	  this
	}
  
  /**
   * when you construct a amandroid procedure instance, call this init function first
   */
  
  def init(name : String, sig : String, paramTyps : List[Type], returnTyp : Type, accessFlags : Int) : JawaProcedure = {
	  init(name, sig, paramTyps, returnTyp, accessFlags, List())
	}
  
  /**
   * when you construct a amandroid procedure instance, call this init function first
   */
  
  def init(name : String, sig : String, paramTyps : List[Type], returnTyp : Type) : JawaProcedure = {
	  init(name, sig, paramTyps, returnTyp, 0, List())
	}
  
  /**
   * when you construct a amandroid procedure instance, call this init function first
   */
  
  def init(sig : String) : JawaProcedure = {
    val name = StringFormConverter.getProcedureNameFromProcedureSignature(sig)
    val recName = StringFormConverter.getRecordNameFromProcedureSignature(sig)
    val sigP = new SignatureParser(sig).getParamSig
    val paramTyps = sigP.getParameterTypes
    val returnTyp = sigP.getReturnType
	  init(name, sig, paramTyps, returnTyp, 0, List())
	}
  
  /**
   * when you construct a amandroid procedure instance, call this init function first
   */
  
  def init(name : String, sig : String) : JawaProcedure = {
    val sigP = new SignatureParser(sig).getParamSig
    val recName = StringFormConverter.getRecordNameFromProcedureSignature(sig)
    val paramTyps = sigP.getParameterTypes
    val returnTyp = sigP.getReturnType
	  init(name, sig, paramTyps, returnTyp, 0, List())
	}
  
  /**
   * get short name of this procedure
   */
  
  def getShortName : String = this.shortName
  
  def getShortName(name : String) : String = {
    if(isFullName(name)) name.substring(name.lastIndexOf('.') + 1)
    else throw new RuntimeException("procedure name not correct: " + name)
  }

  def isFullName(name : String) : Boolean = name.lastIndexOf('.') > 0
  
  /**
   * get name of this procedure
   */
  
  def getName : String = this.name
  
  /**
	 * set declaring record of this field
	 */
  
  def setDeclaringRecord(declRecord : JawaRecord) = if(declRecord != null) this.declaringRecord = declRecord
	
  /**
	 * clear declaring record of this field
	 */
  
  def clearDeclaringRecord = this.declaringRecord = null
  
  /**
   * return the record which declares the current procedure
   */
  
  def getDeclaringRecord = {
    if(!isDeclared) throw new RuntimeException("no declaring record: " + getName)
    declaringRecord
  }
  
  /**
   * set the name of this procedure
   */
  
  def setName(name : String) = {
    val wasDeclared = isDeclared
    val oldDeclaringRecord = declaringRecord
    if(wasDeclared) oldDeclaringRecord.removeProcedure(this)
    this.name = name
    this.shortName = getShortName(name)
    if(wasDeclared) oldDeclaringRecord.addProcedure(this)
  }
  
  /**
   * set signature of this procedure
   */
  
  def setSignature(sig : String) = {
    if(checkSignature(sig)){
	    this.signature = sig
	    this.subSignature = getSubSignature
    } else throw new RuntimeException("not a full-qualified signature: " + sig)
  }
  
  protected def checkSignature(sig : String) = sig.lastIndexOf('.') > 0
  
  /**
   * get signature of this procedure
   */
  
  def getSignature : String = this.signature
  
  /**
   * get sub-signature of this procedure
   */
  
  def getSubSignature : String = {
    if(this.signature != null) {
      this.signature.substring(this.signature.lastIndexOf('.') + 1, this.signature.length())
    } else {
      generateSubSignature
    }
  }
  
  /**
   * generate signature of this procedure
   */
  
  def generateSignature : String = {
    val sb : StringBuffer = new StringBuffer
    if(this.declaringRecord != null){
	    val dc = this.declaringRecord
	    sb.append(StringFormConverter.formatTypeToSigForm(dc.getName))
	    sb.append("." + generateSubSignature)
	    sb.toString().intern()
    } else throw new RuntimeException("not declared: " + this.name)
  }
  
  /**
   * generate sub-signature of this procedure
   */
  
  def generateSubSignature : String = {
    val sb : StringBuffer = new StringBuffer
    val rt = getReturnType
    val pts = getParamTypes
    sb.append(this.shortName + ":(")
    for(i <- 0 to pts.size - 1){
      if(i != 0){
	      val pt = pts(i) 
	      sb.append(StringFormConverter.formatTypeToSigForm(pt.typ))
      }
    }
    sb.append(StringFormConverter.formatTypeToSigForm(rt.typ))
    sb.toString().intern()
  }
  
  /**
   * set return type of this procedure
   */
  
  def setReturnType(typ : Type) = {
    val wasDeclared = isDeclared
    val oldDeclaringRecord = declaringRecord
    if(wasDeclared) oldDeclaringRecord.removeProcedure(this)
    this.returnTyp = typ
    this.signature = generateSignature
    this.subSignature = generateSubSignature
    if(wasDeclared) oldDeclaringRecord.addProcedure(this)
  }
  
  /**
   * set parameter types of this procedure
   */
  
  def setParameterTypes(typs : List[Type]) = {
    val wasDeclared = isDeclared
    val oldDeclaringRecord = declaringRecord
    if(wasDeclared) oldDeclaringRecord.removeProcedure(this)
    this.paramTyps = typs
    this.signature = generateSignature
    this.subSignature = generateSubSignature
    if(wasDeclared) oldDeclaringRecord.addProcedure(this)
  }
  
  /**
   * get return type of this procedure
   */
  
  def getReturnType = this.returnTyp
  
  /**
   * get paramTypes of this procedure
   */
  
  def getParamTypes = {
    if(!isStatic){
      StringFormConverter.getTypeFromName(getDeclaringRecord.getName) :: this.paramTyps
    } else this.paramTyps
  }
  
  /**
   * get i'th parameter's type of this procedure
   */
  
  def getParamType(i : Int) = getParamTypes(i)
  
  /**
   * set parameter names of this procedure
   */
  
  def setParameterNames(names : List[String]) = {
    this.paramNames = names
  }
  
  /**
   * get paramTypes of this procedure
   */
  
  def getParamNames = this.paramNames
  
  /**
   * get i'th parameter's type of this procedure
   */
  
  def getParamName(i : Int) = this.paramNames(i)
  
  /**
	 * return the access flags for this procedure
	 */
	
	def getAccessFlags = accessFlags
	
	/**
	 * get access flag string
	 */
	
	def getAccessFlagString = AccessFlag.toString(getAccessFlags)
	
	/**
	 * sets the access flags for this procedure
	 */
	
	def setAccessFlags(af : Int) = this.accessFlags = af
	
	/**
	 * sets the access flags for this procedure
	 */
	
	def setAccessFlags(str : String) = this.accessFlags = AccessFlag.getAccessFlags(str)
	
	/**
	 * set procedure body
	 */
	
	def setProcedureBody(pb : ProcedureBody) = this.procBody = pb
	
	/**
	 * retrieve code belong to this procedure
	 */
	
	def retrieveCode = if(!isUnknown) JawaCodeSource.getProcedureCode(getSignature) else throw new RuntimeException("Trying to retreive body code for a unknown procedure: " + this)
	
  /**
   * resolve current procedure to body level
   */
  
  def resolveBody = {
    if(this.resolvingLevel >= Center.ResolveLevel.BODY) throw new RuntimeException(getSignature +" is already resolved to " + this.resolvingLevel)
    if(isUnknown) throw new RuntimeException(getSignature + " is a unknown procedure so cannot be resolved to body.")
    val pb = JawaResolver.resolveProcedureBody(getSignature)
    setProcedureBody(pb)
    setResolvingLevel(Center.ResolveLevel.BODY)
    getDeclaringRecord.updateResolvingLevel
  }
  
  /**
   * resolve current procedure to body level
   */
  
  def tryResolveBody = {
    if(isUnknown) throw new RuntimeException("Trying to get the body for a unknown procedure: " + this)
    if(!checkLevel(Center.ResolveLevel.BODY)) resolveBody
  }
  
	/**
	 * get procedure body
	 */
	
	def getProcedureBody = {
	  tryResolveBody
	  this.procBody
	}
  
  /**
   * check procedure body available or not
   */
  
  def hasProcedureBody = this.procBody != null
  
  /**
   * clear procedure body
   */
  
  def clearProcedureBody = this.procBody = null
  
  /**
   * Adds exception which can be thrown by this procedure
   */
  
  def addExceptionIfAbsent(exc : JawaRecord) = {
    if(!throwsException(exc)) addException(exc)
  }
  
  /**
   * Adds exception thrown by this procedure
   */
  
  def addException(exc : JawaRecord) = {
    if(DEBUG) println("Adding Exception: " + exc)
    if(throwsException(exc)) throw new RuntimeException("already throwing exception: " + exc)
    this.exceptions += exc
  }
  
  /**
   * set exception with details
   */
  
  def addExceptionHandler(excName : String, fromTarget : String, toTarget : String, jumpTo : String) = {
    val recName = if(excName == "any") Center.DEFAULT_TOPLEVEL_OBJECT else excName
    val exc = Center.resolveRecord(recName, Center.ResolveLevel.HIERARCHY)
    val handler = ExceptionHandler(exc, fromTarget, toTarget, jumpTo)
    if(DEBUG) println("Adding Exception Handler: " + handler)
    if(this.exceptionHandlers.contains(handler)) throw new RuntimeException("already have exception handler: " + handler)
    addExceptionIfAbsent(exc)
    this.exceptionHandlers ::= handler
  }
  
  /**
   * removes exception from this procedure
   */
  
  def removeException(exc : JawaRecord) = {
    if(DEBUG) println("Removing Exception: " + exc)
    if(!throwsException(exc)) throw new RuntimeException("does not throw exception: " + exc)
    this.exceptions -= exc
    this.exceptionHandlers = this.exceptionHandlers.filter(_.exception != exc)
  }
  
  /**
   * throws this exception or not?
   */
  
  def throwsException(exc : JawaRecord) = this.exceptions.contains(exc)
  
  /**
   * get thrown exception target location
   */
  
  def getThrownExcetpionTarget(exc : JawaRecord, locUri : String) : Option[String] = {
    this.exceptionHandlers.foreach{
      han =>
        if(han.handleException(exc, locUri)) return Some(han.jumpTo)
    }
    err_msg_detail(TITLE, "Given exception " + exc + " throw from " + locUri + ", cannot find a handler to handle it.")
    None
  }
  
  /**
   * set exceptions for this procedure
   */
  
  def setExceptions(excs : Set[JawaRecord]) = this.exceptions = excs
  
  /**
   * get exceptions
   */
  
  def getExceptions = this.exceptions
  
  /**
   * return true if this procedure is concrete which means it is not abstract nor native nor unknown
   */
  
  def isConcrete = !isAbstract && !isNative && !isUnknown
    
  /**
   * return true if this procedure is abstract
   */
  
  def isAbstract : Boolean = AccessFlag.isAbstract(this.accessFlags)
  
  /**
   * return true if this procedure is native
   */
  
  def isNative : Boolean = AccessFlag.isNative(this.accessFlags)
  
  /**
   * return true if this procedure is static
   */
  
  def isStatic : Boolean = AccessFlag.isStatic(this.accessFlags)
  
  /**
   * return true if this procedure is private
   */
  
  def isPrivate : Boolean = AccessFlag.isPrivate(this.accessFlags)
  
  /**
   * return true if this procedure is public
   */
  
  def isPublic : Boolean = AccessFlag.isPublic(this.accessFlags)
  
  /**
   * return true if this procedure is protected
   */
  
  def isProtected : Boolean = AccessFlag.isProtected(this.accessFlags)
  
  /**
   * return true if this procedure is final
   */
  
  def isFinal : Boolean = AccessFlag.isFinal(this.accessFlags)
  
  /**
   * return true if this procedure is synchronized
   */
  
  def isSynchronized : Boolean = AccessFlag.isSynchronized(this.accessFlags)
  
  /**
   * return true if this procedure is synthetic
   */
  
  def isSynthetic : Boolean = AccessFlag.isSynthetic(this.accessFlags)
  
  /**
   * return true if this procedure is constructor
   */
  
  def isConstructor : Boolean = AccessFlag.isConstructor(this.accessFlags)
  
  /**
   * return true if this procedure is declared_synchronized
   */
  
  def isDeclaredSynchronized : Boolean = AccessFlag.isDeclaredSynchronized(this.accessFlags)
  
  /**
   * return true if this procedure is main procedure
   */
  
  def isMain : Boolean = isPublic && isStatic && this.subSignature == "main:([Ljava/lang/string;)V"
    
  /**
   * return false if this procedure is a special one with empty body. e.g. A.class
   */
    
  def isUnknown : Boolean = this.unknown
  
  /**
   * set the reality of the procedure. e.g. for A.class we set the reality as false
   */
  
  def setUnknown = this.unknown = true
    
  /**
   * return true if this procedure is a record initializer or main function
   */
    
  def isEntryProcedure = {
    if(isStatic && name == getDeclaringRecord.staticInitializerName) true
    else isMain
  }
  
  def printDetail = {
    println("--------------AmandroidProcedure--------------")
    println("procName: " + getName)
    println("shortName: " + getShortName)
    println("signature: " + getSignature)
    println("subSignature: " + getSubSignature)
    println("declaringRecord: " + getDeclaringRecord)
    println("paramTypes: " + getParamTypes)
    println("accessFlags: " + getAccessFlagString)
    println("----------------------------")
  }
  
  override def toString : String = getSignature
  
}