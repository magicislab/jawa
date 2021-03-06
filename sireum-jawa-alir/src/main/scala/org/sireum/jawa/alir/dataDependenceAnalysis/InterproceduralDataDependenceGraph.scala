/*
Copyright (c) 2013-2014 Fengguo Wei & Sankardas Roy, Kansas State University.        
All rights reserved. This program and the accompanying materials      
are made available under the terms of the Eclipse Public License v1.0 
which accompanies this distribution, and is available at              
http://www.eclipse.org/legal/epl-v10.html                             
*/
package org.sireum.jawa.alir.dataDependenceAnalysis

import org.sireum.jawa._
import org.sireum.jawa.alir.controlFlowGraph._
import org.sireum.jawa.alir.Context
import org.sireum.pilar.ast._
import org.sireum.util._
import org.sireum.jawa.alir.interProcedural.InstanceCallee
import org.sireum.jawa.alir.interProcedural.InterProceduralNode
import org.sireum.jawa.alir.interProcedural.InterProceduralGraph
import org.sireum.jawa.util.ASTUtil

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
class InterProceduralDataDependenceGraph[Node <: IDDGNode] extends InterProceduralGraph[Node]{
	
  protected var centerN : IDDGCenterNode = null
  def centerNode : Node = this.centerN.asInstanceOf[Node]
  
  protected var entryN : IDDGEntryNode = null
  def entryNode : Node = this.entryN.asInstanceOf[Node]
  
  protected var cg : InterproceduralControlFlowGraph[CGNode] = null
  
	def initGraph(cg : InterproceduralControlFlowGraph[CGNode]) = {
    this.cg = cg
	  cg.nodes.foreach{
	    node =>
	      node match{
	        case en : CGEntryNode =>
	          val owner = Center.getProcedureWithoutFailing(en.getOwner)
	          val pnames = owner.getParamNames
	          val ptyps = owner.getParamTypes
	          var position = 0
	          for(i <- 0 to pnames.size - 1){
	            val ptypName = ptyps(i).name
	            val n = addIDDGEntryParamNode(en, position)
	            n.asInstanceOf[IDDGEntryParamNode].paramName = pnames(i)
	            if(ptypName == "double" || ptypName == "long"){
	              position += 1
	              val n = addIDDGEntryParamNode(en, position)
	              n.asInstanceOf[IDDGEntryParamNode].paramName = pnames(i)
	            }
	            position += 1
	          }
	        case en : CGExitNode =>
	          val owner = Center.getProcedureWithoutFailing(en.getOwner)
	          val pnames = owner.getParamNames
            val ptyps = owner.getParamTypes
            var position = 0
            for(i <- 0 to pnames.size - 1){
              val ptypName = ptyps(i).name
              val n = addIDDGExitParamNode(en, position)
              n.asInstanceOf[IDDGExitParamNode].paramName = pnames(i)
              if(ptypName == "double" || ptypName == "long"){
                position += 1
                val n = addIDDGExitParamNode(en, position)
                n.asInstanceOf[IDDGExitParamNode].paramName = pnames(i)
              }
              position += 1
            }
	        case en : CGCenterNode =>
	        case cn : CGCallNode =>
	          val loc = Center.getProcedureWithoutFailing(cn.getOwner).getProcedureBody.location(cn.getLocIndex)
	          val argNames : MList[String] = mlistEmpty
	          loc match{
	            case jumploc : JumpLocation =>
	              argNames ++= ASTUtil.getCallArgs(jumploc)
	            case _ =>
	          }
	          for(i <- 0 to (argNames.size - 1)){
	            val argName = argNames(i)
	            val n = addIDDGCallArgNode(cn, i)
	            n.asInstanceOf[IDDGCallArgNode].argName = argName
	          }
	          val rn = addIDDGReturnVarNode(cn)
	          if(cn.getCalleeSet.exists{p => p.callee.getDeclaringRecord.isFrameworkRecord || p.callee.getDeclaringRecord.isThirdPartyLibRecord}){
	            val vn = addIDDGVirtualBodyNode(cn)
	            vn.asInstanceOf[IDDGVirtualBodyNode].argNames = argNames.toList
	          }
	        case rn : CGReturnNode =>
	          val loc =  Center.getProcedureWithoutFailing(rn.getOwner).getProcedureBody.location(rn.getLocIndex)
	          val argNames : MList[String] = mlistEmpty
	          loc match{
	            case jumploc : JumpLocation =>
	              argNames ++= ASTUtil.getCallArgs(jumploc)
	            case _ =>
	          }
	          for(i <- 0 to (argNames.size - 1)){
	            val argName = argNames(i)
	            val n = addIDDGReturnArgNode(rn, i)
	            n.asInstanceOf[IDDGReturnArgNode].argName = argName
	          }
	        case nn : CGNormalNode => addIDDGNormalNode(nn)
	        case _ =>
	      }
	  }
    this.centerN = addIDDGCenterNode(cg.centerNode.asInstanceOf[CGCenterNode]).asInstanceOf[IDDGCenterNode]
    this.entryN = addIDDGEntryNode(cg.entryNode.asInstanceOf[CGEntryNode]).asInstanceOf[IDDGEntryNode]
	}
	
	def findDefSite(defSite : Context) : Node = {
	  val cgN = {
	    if(this.cg.cgNormalNodeExists(defSite)) this.cg.getCGNormalNode(defSite)
		  else if(this.cg.cgCallNodeExists(defSite)) this.cg.getCGCallNode(defSite)
		  else if(defSite.toString == "(EntryPoint,L0000)") this.cg.entryNode
		  else if(defSite.toString == "(Center,L0000)") this.cg.centerNode
		  else throw new RuntimeException("Cannot find node: " + defSite)
	  }
	  if(cgN.isInstanceOf[CGNormalNode] && iddgNormalNodeExists(cgN.asInstanceOf[CGNormalNode])) getIDDGNormalNode(cgN.asInstanceOf[CGNormalNode])
	  else if(cgN.isInstanceOf[CGCallNode] && iddgVirtualBodyNodeExists(cgN.asInstanceOf[CGCallNode])) getIDDGVirtualBodyNode(cgN.asInstanceOf[CGCallNode])
	  else if(cgN.isInstanceOf[CGCallNode] && iddgReturnVarNodeExists(cgN.asInstanceOf[CGCallNode])) getIDDGReturnVarNode(cgN.asInstanceOf[CGCallNode])
	  else if(cgN == this.cg.entryNode) this.entryNode
	  else if(cgN == this.cg.centerNode) this.centerNode
	  else throw new RuntimeException("Cannot find node: " + defSite)
	}
	
	def findDefSite(defSite : Context, position : Int) : Node = {
	  val cgN = {
	    if(this.cg.cgCallNodeExists(defSite)) this.cg.getCGCallNode(defSite)
	    else if(this.cg.cgReturnNodeExists(defSite)) this.cg.getCGReturnNode(defSite)
	    else if(this.cg.cgEntryNodeExists(defSite)) this.cg.getCGEntryNode(defSite)
	    else if(this.cg.cgExitNodeExists(defSite)) this.cg.getCGExitNode(defSite)
		  else throw new RuntimeException("Cannot find node: " + defSite)
	  }
	  if(cgN.isInstanceOf[CGCallNode] && iddgCallArgNodeExists(cgN.asInstanceOf[CGCallNode], position)) getIDDGCallArgNode(cgN.asInstanceOf[CGCallNode], position)
	  else if(cgN.isInstanceOf[CGReturnNode] && iddgReturnArgNodeExists(cgN.asInstanceOf[CGReturnNode], position)) getIDDGReturnArgNode(cgN.asInstanceOf[CGReturnNode], position)
	  else if(cgN.isInstanceOf[CGEntryNode] && iddgEntryParamNodeExists(cgN.asInstanceOf[CGEntryNode], position)) getIDDGEntryParamNode(cgN.asInstanceOf[CGEntryNode], position)
	  else if(cgN.isInstanceOf[CGExitNode] && iddgExitParamNodeExists(cgN.asInstanceOf[CGExitNode], position)) getIDDGExitParamNode(cgN.asInstanceOf[CGExitNode], position)
	  else throw new RuntimeException("Cannot find node: " + defSite + ":" + position)
	}
  
  def iddgEntryParamNodeExists(cgN : CGEntryNode, position : Int) : Boolean = {
    graph.containsVertex(newIDDGEntryParamNode(cgN, position).asInstanceOf[Node])
  }

  def addIDDGEntryParamNode(cgN : CGEntryNode, position : Int) : Node = {
    val node = newIDDGEntryParamNode(cgN, position).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }

  def getIDDGEntryParamNode(cgN : CGEntryNode, position : Int) : Node =
    pool(newIDDGEntryParamNode(cgN, position))
    
  protected def newIDDGEntryParamNode(cgN : CGEntryNode, position : Int) =
    IDDGEntryParamNode(cgN, position)
	
  def iddgExitParamNodeExists(cgN : CGExitNode, position : Int) : Boolean = {
    graph.containsVertex(newIDDGExitParamNode(cgN, position).asInstanceOf[Node])
  }

  def addIDDGExitParamNode(cgN : CGExitNode, position : Int) : Node = {
    val node = newIDDGExitParamNode(cgN, position).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }

  def getIDDGExitParamNode(cgN : CGExitNode, position : Int) : Node =
    pool(newIDDGExitParamNode(cgN, position))
    
  protected def newIDDGExitParamNode(cgN : CGExitNode, position : Int) =
    IDDGExitParamNode(cgN, position)
    
  def iddgCallArgNodeExists(cgN : CGCallNode, position : Int) : Boolean = {
    graph.containsVertex(newIDDGCallArgNode(cgN, position).asInstanceOf[Node])
  }

  def addIDDGCallArgNode(cgN : CGCallNode, position : Int) : Node = {
    val node = newIDDGCallArgNode(cgN, position).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }

  def getIDDGCallArgNode(cgN : CGCallNode, position : Int) : Node =
    pool(newIDDGCallArgNode(cgN, position))
    
  def getIDDGCallArgNodes(cgN : CGCallNode) : Set[Node] = {
    val result : MSet[Node] = msetEmpty
    var position = 0
    while(iddgCallArgNodeExists(cgN, position)){
    	result += pool(newIDDGCallArgNode(cgN, position))
    	position += 1
    }
    result.toSet
  }
    
  protected def newIDDGCallArgNode(cgN : CGCallNode, position : Int) = IDDGCallArgNode(cgN, position)
    
  def iddgReturnArgNodeExists(cgN : CGReturnNode, position : Int) : Boolean = {
    graph.containsVertex(newIDDGReturnArgNode(cgN, position).asInstanceOf[Node])
  }

  def addIDDGReturnArgNode(cgN : CGReturnNode, position : Int) : Node = {
    val node = newIDDGReturnArgNode(cgN, position).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }

  def getIDDGReturnArgNode(cgN : CGReturnNode, position : Int) : Node =
    pool(newIDDGReturnArgNode(cgN, position))
    
  protected def newIDDGReturnArgNode(cgN : CGReturnNode, position : Int) = IDDGReturnArgNode(cgN, position)
    
  def iddgReturnVarNodeExists(cgN : CGCallNode) : Boolean = {
    graph.containsVertex(newIDDGReturnVarNode(cgN).asInstanceOf[Node])
  }

  def addIDDGReturnVarNode(cgN : CGCallNode) : Node = {
    val node = newIDDGReturnVarNode(cgN).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }

  def getIDDGReturnVarNode(cgN : CGCallNode) : Node =
    pool(newIDDGReturnVarNode(cgN))
    
  protected def newIDDGReturnVarNode(cgN : CGCallNode) =
    IDDGReturnVarNode(cgN)
    
  def iddgVirtualBodyNodeExists(cgN : CGCallNode) : Boolean = {
    graph.containsVertex(newIDDGVirtualBodyNode(cgN).asInstanceOf[Node])
  }
  
  def addIDDGVirtualBodyNode(cgN : CGCallNode) : Node = {
    val node = newIDDGVirtualBodyNode(cgN).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }

  def getIDDGVirtualBodyNode(cgN : CGCallNode) : Node =
    pool(newIDDGVirtualBodyNode(cgN))
    
  protected def newIDDGVirtualBodyNode(cgN : CGCallNode) =
    IDDGVirtualBodyNode(cgN)
    
  def iddgNormalNodeExists(cgN : CGNormalNode) : Boolean = {
    graph.containsVertex(newIDDGNormalNode(cgN).asInstanceOf[Node])
  }
  
  def addIDDGNormalNode(cgN : CGNormalNode) : Node = {
    val node = newIDDGNormalNode(cgN).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }

  def getIDDGNormalNode(cgN : CGNormalNode) : Node =
    pool(newIDDGNormalNode(cgN))
    
  protected def newIDDGNormalNode(cgN : CGNormalNode) =
    IDDGNormalNode(cgN)
    
  def iddgCenterNodeExists(cgN : CGCenterNode) : Boolean = {
    graph.containsVertex(newIDDGCenterNode(cgN).asInstanceOf[Node])
  }
  
  def addIDDGCenterNode(cgN : CGCenterNode) : Node = {
    val node = newIDDGCenterNode(cgN).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }

  def getIDDGCenterNode(cgN : CGCenterNode) : Node =
    pool(newIDDGCenterNode(cgN))
    
  protected def newIDDGCenterNode(cgN : CGCenterNode) =
    IDDGCenterNode(cgN)
    
  def iddgEntryNodeExists(cgN : CGEntryNode) : Boolean = {
    graph.containsVertex(newIDDGEntryNode(cgN).asInstanceOf[Node])
  }
  
  def addIDDGEntryNode(cgN : CGEntryNode) : Node = {
    val node = newIDDGEntryNode(cgN).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }

  def getIDDGCenterNode(cgN : CGEntryNode) : Node =
    pool(newIDDGEntryNode(cgN))
    
  protected def newIDDGEntryNode(cgN : CGEntryNode) =
    IDDGEntryNode(cgN)
}

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
sealed abstract class IDDGNode(cgN : CGNode) extends InterProceduralNode(cgN.getContext) {
  def getCGNode = cgN
  def getOwner = cgN.getOwner
  def getCode : String = cgN.getCode
  override def getContext = cgN.getContext
}

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
abstract class IDDGVirtualNode(cgN : CGNode) extends IDDGNode(cgN) 

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
abstract class IDDGLocNode(cgN : CGLocNode) extends IDDGNode(cgN) {
  def getLocUri = cgN.getLocUri
  def getLocIndex : Int = cgN.getLocIndex
}

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
abstract class IDDGInvokeNode(cgN : CGInvokeNode) extends IDDGLocNode(cgN) {
  def getCalleeSet = cgN.getCalleeSet
}

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
final case class IDDGNormalNode(cgN : CGNormalNode) extends IDDGLocNode(cgN) 

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
final case class IDDGEntryParamNode(cgN : CGEntryNode, position : Int) extends IDDGVirtualNode(cgN){
  var paramName : String = null
  def getVirtualLabel : String = "EntryParam:" + position
}

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
final case class IDDGCenterNode(cgN : CGCenterNode) extends IDDGVirtualNode(cgN){
  def getVirtualLabel : String = "Center"
}

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
final case class IDDGEntryNode(cgN : CGEntryNode) extends IDDGVirtualNode(cgN)

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
final case class IDDGExitParamNode(cgN : CGExitNode, position : Int) extends IDDGVirtualNode(cgN){
  var paramName : String = null
  def getVirtualLabel : String = "ExitParam:" + position
}

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
final case class IDDGVirtualBodyNode(cgN : CGCallNode) extends IDDGInvokeNode(cgN){
  var argNames : List[String] = null
  def getInvokeLabel : String = "VirtualBody"
}

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
final case class IDDGCallArgNode(cgN : CGCallNode, position : Int) extends IDDGInvokeNode(cgN){
  var argName : String = null
  def getInvokeLabel : String = "CallArg:" + position
}

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
final case class IDDGReturnArgNode(cgN : CGReturnNode, position : Int) extends IDDGInvokeNode(cgN){
  var argName : String = null
  def getInvokeLabel : String = "ReturnArg:" + position
}

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
final case class IDDGReturnVarNode(cgN : CGCallNode) extends IDDGInvokeNode(cgN){
  def getInvokeLabel : String = "ReturnVar"
}