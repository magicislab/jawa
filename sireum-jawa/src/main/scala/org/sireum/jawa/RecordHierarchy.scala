/*
Copyright (c) 2013-2014 Fengguo Wei & Sankardas Roy, Kansas State University.        
All rights reserved. This program and the accompanying materials      
are made available under the terms of the Eclipse Public License v1.0 
which accompanies this distribution, and is available at              
http://www.eclipse.org/legal/epl-v10.html                             
*/
package org.sireum.jawa

import org.sireum.util._
import org.sireum.jawa.util.StringFormConverter

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 */ 
class RecordHierarchy {
	/**
	 * this map is from class to it's sub-classes.
	 */
  
  protected val classToSubClasses : MMap[JawaRecord, MSet[JawaRecord]] = mmapEmpty
  
  /**
   * this map is from interface to sub-interfaces.
   */
  
  protected val interfaceToSubInterfaces : MMap[JawaRecord, MSet[JawaRecord]] = mmapEmpty
  
  /**
   * this map is from class to all sub-classes.  Not filled in inside the build()
   */
  
  protected val classToAllSubClasses : MMap[JawaRecord, MSet[JawaRecord]] = mmapEmpty
  
  /**
   * this map is from interface to all sub-interfaces. Not filled in inside the build()
   */
  
  protected val interfaceToAllSubInterfaces : MMap[JawaRecord, MSet[JawaRecord]] = mmapEmpty
  
  /**
   * this map is from interface to direct implementers
   */
  
  protected val interfaceToImplememters : MMap[JawaRecord, MSet[JawaRecord]] = mmapEmpty
  
  /**
   * construct a hierarchy from the current scene i.e. Center
   */
  
  def build : RecordHierarchy = {
    val allRecords = Center.getRecords
    allRecords.foreach{
      record =>
        if(record.hasSuperClass){
          if(record.isInterface){
            record.getInterfaces.foreach{this.interfaceToSubInterfaces.getOrElseUpdate(_, msetEmpty) += record}
          } else {
            this.classToSubClasses.getOrElseUpdate(record.getSuperClass, msetEmpty) += record
            record.getInterfaces.foreach{this.interfaceToImplememters.getOrElseUpdate(_, msetEmpty) += record}
          }
        }
    }
    // fill in the implementers sets with subclasses
    allRecords.foreach{
      record =>
        if(record.isInterface){
          val imps = this.interfaceToImplememters.getOrElseUpdate(record, msetEmpty)
          if(!imps.isEmpty)
          	imps ++= imps.map{getAllSubClassesOfIncluding(_)}.reduce((s1, s2) => s1 ++ s2)
        }
    }
    this
  }
  
  /**
   * return a set of all sub-classes of r, including itself
   */
  
  def getAllSubClassesOfIncluding(r : JawaRecord) : Set[JawaRecord] = {
    if(r.isInterface) throw new RuntimeException("r need to be class type: " + r)
    getAllSubClassesOf(r) + r
  }
  
  /**
   * return a set of all sub-classes of r
   */
  
  def getAllSubClassesOf(r : JawaRecord) : Set[JawaRecord] = {
    if(r.isInterface) throw new RuntimeException("r need to be class type: " + r)
    this.classToAllSubClasses.get(r) match{
      case Some(records) => records.toSet //if already cached return the value
      case None => 
        val subRecords = this.classToSubClasses.getOrElseUpdate(r, msetEmpty)
        if(!subRecords.isEmpty){
	        val allSubRecords = subRecords.map{getAllSubClassesOfIncluding(_)}.reduce((s1, s2) => s1 ++ s2)
	        this.classToAllSubClasses.getOrElseUpdate(r, msetEmpty) ++= allSubRecords
	        allSubRecords
        } else Set()
    }
  }
  
  /**
   * return a set of all super-classes of r, including itself
   */
  
  def getAllSuperClassesOfIncluding(r : JawaRecord) : Set[JawaRecord] = {
    if(r.isInterface) throw new RuntimeException("r need to be class type: " + r)
    getAllSuperClassesOf(r) + r
  }
  
  /**
   * return a set of all super-classes of r
   */
  
  def getAllSuperClassesOf(r : JawaRecord) : Set[JawaRecord] = {
    if(r.isInterface) throw new RuntimeException("r need to be class type: " + r)
    var rl = r
    var l : Set[JawaRecord] = Set()
    while(rl.hasSuperClass){
      l += rl.getSuperClass
      rl = rl.getSuperClass
    }
    l
  }
  
  /**
   * return a set of all sub-interfaces of r, including itself
   */
  
  def getAllSubInterfacesOfIncluding(r : JawaRecord) : Set[JawaRecord] = {
    if(!r.isInterface) throw new RuntimeException("r need to be interface type: " + r)
    getAllSubInterfacesOf(r) + r
  }
  
  /**
   * return a set of all sub-interfaces of r
   */
  
  def getAllSubInterfacesOf(r : JawaRecord) : Set[JawaRecord] = {
    if(!r.isInterface) throw new RuntimeException("r need to be interface type: " + r)
    this.interfaceToAllSubInterfaces.get(r) match{
      case Some(records) => records.toSet //if already cached return the value
      case None => 
        val subRecords = this.interfaceToSubInterfaces.getOrElseUpdate(r, msetEmpty)
        if(!subRecords.isEmpty){
	        val allSubRecords = subRecords.map{getAllSubInterfacesOfIncluding(_)}.reduce((s1, s2) => s1 ++ s2)
	        this.interfaceToAllSubInterfaces.getOrElseUpdate(r, msetEmpty) ++= allSubRecords
	        allSubRecords
        } else Set()
    }
  }
  
  /**
   * return a set of all super-interfaces of r, including itself
   */
  
  def getAllSuperInterfacesOfIncluding(r : JawaRecord) : Set[JawaRecord] = {
    if(!r.isInterface) throw new RuntimeException("r need to be interface type: " + r)
    getAllSuperInterfacesOf(r) + r
  }
  
  /**
   * return a set of all super-interfaces of r
   */
  
  def getAllSuperInterfacesOf(r : JawaRecord) : Set[JawaRecord] = {
    if(!r.isInterface) throw new RuntimeException("r need to be interface type: " + r)
    val ins = r.getInterfaces
    if(!ins.isEmpty)
    	ins.map{getAllSuperInterfacesOf(_)}.reduce((s1, s2) => s1 ++ s2) ++ ins
    else
      ins
  }
  
  /**
   * return a set of sub-classes of r, including itself
   */
  
  def getSubClassesOfIncluding(r : JawaRecord) : Set[JawaRecord] = {
    if(r.isInterface) throw new RuntimeException("r need to be class type: " + r)
    getSubClassesOf(r) + r
  }
  
  /**
   * return a set of sub-classes of r
   */
  
  def getSubClassesOf(r : JawaRecord) : Set[JawaRecord] = {
    if(r.isInterface) throw new RuntimeException("r need to be class type: " + r)
    this.classToSubClasses.getOrElse(r, msetEmpty).toSet
  }
  
  /**
   * return super-classe of r
   */
  
  def getSuperClassOf(r : JawaRecord) : JawaRecord = {
    if(r.isInterface) throw new RuntimeException("r need to be class type: " + r)
    r.getSuperClass
  }
  
  /**
   * return a set of sub-interfaces of r, including itself
   */
  
  def getSubInterfacesOfIncluding(r : JawaRecord) : Set[JawaRecord] = {
    if(!r.isInterface) throw new RuntimeException("r need to be interface type: " + r)
    getSubInterfacesOf(r) + r
  }
  
  /**
   * return a set of sub-interfaces of r
   */
  
  def getSubInterfacesOf(r : JawaRecord) : Set[JawaRecord] = {
    if(!r.isInterface) throw new RuntimeException("r need to be interface type: " + r)
    this.interfaceToSubInterfaces.getOrElse(r, msetEmpty).toSet
  }
  
  /**
   * return a set of all super-interfaces of r
   */
  
  def getSuperInterfacesOf(r : JawaRecord) : Set[JawaRecord] = {
    if(!r.isInterface) throw new RuntimeException("r need to be interface type: " + r)
    r.getInterfaces
  }
  
  /**
   * get all implementers of r
   */
  
  def getAllImplementersOf(r : JawaRecord) : Set[JawaRecord] = {
    if(!r.isInterface) throw new RuntimeException("r need to be interface type: " + r)
    val subI = getSubInterfacesOfIncluding(r)
    if(!subI.isEmpty)
    	subI.map{getImplementersOf(_)}.reduce((s1, s2) => s1 ++ s2)
    else Set()
  }
  
  /**
   * get implementers of r
   */
  
  def getImplementersOf(r : JawaRecord) : Set[JawaRecord] = {
    if(!r.isInterface) throw new RuntimeException("r need to be interface type: " + r)
    this.interfaceToImplememters.getOrElse(r, msetEmpty).toSet
  }
  
  /**
   * return true if child is a subclass of given parent recursively
   */
  
  def isRecordRecursivelySubClassOf(child : JawaRecord, parent : JawaRecord) : Boolean = {
    getAllSuperClassesOf(child).contains(parent)
  }
  
   /**
   * return true if child is a subclass of given parent recursively
   */
  
  def isRecordRecursivelySubClassOfIncluding(child : JawaRecord, parent : JawaRecord) : Boolean = {
    getAllSuperClassesOfIncluding(child).contains(parent)
  }
  
  /**
   * return true if child is a subclass of given parent
   */
  
  def isRecordSubClassOf(child : JawaRecord, parent : JawaRecord) : Boolean = {
    if(child.isInterface) throw new RuntimeException("r need to be class type: " + child)
    getSuperClassOf(child) == parent
  }
  
  /**
   * return true if child is a super class of given parent recursively
   */
  
  def isRecordRecursivelySuperClassOf(parent : JawaRecord, child : JawaRecord) : Boolean = {
    getAllSubClassesOf(parent).contains(child)
  }
  
  /**
   * return true if child is a super class of given parent recursively
   */
  
  def isRecordRecursivelySuperClassOfIncluding(parent : JawaRecord, child : JawaRecord) : Boolean = {
    getAllSubClassesOfIncluding(parent).contains(child)
  }
  
  /**
   * return true if child is a subclass of given parent
   */
  
  def isRecordSuperClassOf(parent : JawaRecord, child : JawaRecord) : Boolean = {
    if(parent.isInterface) throw new RuntimeException("r need to be class type: " + parent)
    child.getSuperClass == parent
  }
  
  /**
   * return true if child is a subinterface of given parent recursively
   */
  
  def isRecordRecursivelySubInterfaceOf(child : JawaRecord, parent : JawaRecord) : Boolean = {
    if(!child.isInterface) throw new RuntimeException("r need to be interface type: " + child)
    getAllSuperInterfacesOf(child).contains(parent)
  }
  
   /**
   * return true if child is a subinterface of given parent recursively
   */
  
  def isRecordRecursivelySubInterfaceOfIncluding(child : JawaRecord, parent : JawaRecord) : Boolean = {
    if(!child.isInterface) throw new RuntimeException("r need to be interface type: " + child)
    getAllSuperInterfacesOfIncluding(child).contains(parent)
  }
  
  /**
   * return true if child is a subinterface of given parent
   */
  
  def isRecordSubInterfaceOf(child : JawaRecord, parent : JawaRecord) : Boolean = {
    if(!child.isInterface) throw new RuntimeException("r need to be interface type: " + child)
    getSuperInterfacesOf(child).contains(parent)
  }
  
  /**
   * return true if the procedure is visible from record from
   */
  
  def isProcedureVisible(from : JawaRecord, p : JawaProcedure) : Boolean = {
    if(p.isPublic) true
    else if(p.isPrivate) p.getDeclaringRecord == from
    else if(p.isProtected) isRecordRecursivelySubClassOfIncluding(from, p.getDeclaringRecord)
    /* If none of these access control accesflag been set, means the method has default or package level access
     * which means this method can be accessed within the class or other classes in the same package.
     */
    else p.getDeclaringRecord == from || p.getDeclaringRecord.getPackageName == from.getPackageName
  }
  
  /**
   * Given an object created by o = new R as type R, return the procedure which will be called by o.p()
   */
  
  def resolveConcreteDispatch(concreteType : JawaRecord, p : JawaProcedure) : JawaProcedure = {
    if(concreteType.isInterface) throw new RuntimeException("concreteType need to be class type: " + concreteType)
    val pSubSig = p.getSubSignature
    resolveConcreteDispatch(concreteType, pSubSig)
  }
  
  /**
   * Given an object created by o = new R as type R, return the procedure which will be called by o.p()
   */
  
  def resolveConcreteDispatch(concreteType : JawaRecord, pSubSig : String) : JawaProcedure = {
    if(concreteType.isInterface) throw new RuntimeException("Receiver need to be class type: " + concreteType)
    findProcedureThroughHierarchy(concreteType, pSubSig) match {
      case Some(ap) => 
        if(ap.isAbstract) throw new RuntimeException("Target procedure needs to be non-abstract method type: " + ap)
        else if(!isProcedureVisible(concreteType, ap)) throw new RuntimeException("Target procedure " + ap + " needs to be visible from: " + concreteType)
        else ap
      case None => throw new RuntimeException("Cannot resolve concrete dispatch!\n" + "Type:" + concreteType + "\nProcedure:" + pSubSig)
    }
  }
  
  private def findProcedureThroughHierarchy(record : JawaRecord, subSig : String) : Option[JawaProcedure] = {
    if(record.isUnknown){
      this.synchronized{
	      record.tryGetProcedure(subSig) match{
	        case Some(p) => Some(p)
	        case None =>
	          val ap = new JawaProcedure
	          ap.init(StringFormConverter.getSigFromOwnerAndProcSubSig(record.getName, subSig))
	          ap.setUnknown
	          record.addProcedure(ap)
	          Some(ap)
	      }
      }
    } else {
	    record.tryGetProcedure(subSig) match{
	      case Some(p) =>
	        Some(p)
	      case None =>
	        if(record.hasSuperClass)
	        	findProcedureThroughHierarchy(record.getSuperClass, subSig)
	        else None
	    }
    }
  }
  
  /**
   * Given an abstract dispatch to an object of type r and a procedure p, gives a list of possible receiver's methods
   */
  
  def resolveAbstractDispatch(r : JawaRecord, pSubSig : String) : Set[JawaProcedure] = {
    val results : MSet[JawaProcedure] = msetEmpty
    val records : MSet[JawaRecord] = msetEmpty
    if(r.isInterface){
      records ++= getAllImplementersOf(r)
    } else {
      records ++= getAllSubClassesOfIncluding(r)
    }
    
    records.filter { r => !r.isAbstract }.foreach{
      rec =>
        findProcedureThroughHierarchy(rec, pSubSig) match {
          case Some(p) => if(!p.isAbstract) results += p
          case None =>
        }
    }
    if(results.isEmpty){
      if(r.isInterface || r.isAbstract){
        findProcedureThroughHierarchy(r, pSubSig) match { //check whether this method is in the java.lang.Object class.
          case Some(p) => if(!p.isAbstract) results += p
          case None => // It's an unknown method since we cannot find any implementer of this interface and such method is getting invoked.
        }
        if(results.isEmpty){
          val unknownrec = new JawaRecord
          unknownrec.init(r.getName + "*")
          unknownrec.setApplicationRecord
          unknownrec.setUnknown
          if(r.isInterface) unknownrec.addInterface(r)
          else if(r.isAbstract) unknownrec.setSuperClass(r)
          val unknownpro = new JawaProcedure
          unknownpro.init(StringFormConverter.getSigFromOwnerAndProcSubSig(unknownrec.getName, pSubSig))
          unknownpro.setUnknown
          unknownrec.addProcedure(unknownpro)
          results += unknownpro
        }
      } else throw new RuntimeException("Could not resolve abstract dispath for:\nclass:" + r + " method:" + pSubSig)
    }
    results.toSet
  }
  
  /**
   * Given an abstract dispatch to an object of type r and a procedure p, gives a list of possible receiver's methods
   */
  
  def resolveAbstractDispatch(r : JawaRecord, p : JawaProcedure) : Set[JawaProcedure] = {
    val pSubSig = p.getSubSignature
    resolveAbstractDispatch(r, pSubSig)
  }
  
  def printDetails = {
    println("==================hierarchy==================")
    println("interfaceToSubInterfaces:\n" + this.interfaceToSubInterfaces)
    println("classToSubClasses:\n" + this.classToSubClasses)
    println("interfaceToImplememters:\n" + this.interfaceToImplememters)
    println("====================================")
  }
  
  override def toString() : String = {
    val sb = new StringBuffer
    sb.append("\ninterface to sub-interfaces:\n")
    this.interfaceToSubInterfaces.foreach{
      case (k, v) =>
        sb.append(k + "->" + v + "\n")
    }
    sb.append("interface to implementers:\n")
    this.interfaceToImplememters.foreach{
      case (k, v) =>
        sb.append(k + "->" + v + "\n")
    }
    sb.append("class to sub-classes:\n")
    this.classToSubClasses.foreach{
      case (k, v) =>
        sb.append(k + "->" + v + "\n")
    }
    sb.toString().intern()
  }
}