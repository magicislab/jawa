/*
Copyright (c) 2013-2014 Fengguo Wei & Sankardas Roy, Kansas State University.        
All rights reserved. This program and the accompanying materials      
are made available under the terms of the Eclipse Public License v1.0 
which accompanies this distribution, and is available at              
http://www.eclipse.org/legal/epl-v10.html                             
*/
package org.sireum.jawa.alir

import org.sireum.util._
import org.sireum.jawa.NullType
import org.sireum.jawa.Type
import org.sireum.jawa.NormalType

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
abstract class Instance{
  def typ : Type
  def getType = typ
  def defSite : Context
  def getDefSite = defSite
  def clone(newDefSite : Context) : Instance
  var fieldsUnknownDefSites : IMap[Context, Set[String]] = imapEmpty
  def addFieldsUnknownDefSite(defSite : Context, fields : Set[String]) = this.fieldsUnknownDefSites += (defSite -> fields)
  def setFieldsUnknownDefSites(defSites : IMap[Context, Set[String]]) = this.fieldsUnknownDefSites = defSites
  def getFieldsUnknownDefSites = this.fieldsUnknownDefSites
}


final case class ClassInstance(name: String, defSite : Context) extends Instance{
  override def clone(newDefSite : Context) : Instance = ClassInstance(name, newDefSite)
  def typ = NormalType("java.lang.Class", 0)
  def getName = name
  override def toString : String = this.name + ".class@" + this.defSite.getCurrentLocUri
}

final case class NullInstance(defSite : Context) extends Instance{
  override def clone(newDefSite : Context) : Instance = NullInstance(newDefSite)
  def typ : Type = new NullType
  override def toString : String = "Null" + "@" + defSite.getCurrentLocUri
}

final case class UnknownInstance(baseTyp : Type, defSite : Context) extends Instance{
  override def clone(newDefSite : Context) : Instance = UnknownInstance(baseTyp, newDefSite)
  def typ : Type = baseTyp
  override def toString : String = baseTyp + "*" + "@" + defSite.getCurrentLocUri
}