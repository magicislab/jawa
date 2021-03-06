/*
Copyright (c) 2013-2014 Fengguo Wei & Sankardas Roy, Kansas State University.        
All rights reserved. This program and the accompanying materials      
are made available under the terms of the Eclipse Public License v1.0 
which accompanies this distribution, and is available at              
http://www.eclipse.org/legal/epl-v10.html                             
*/
package org.sireum.jawa.alir.controlFlowGraph

import org.sireum.alir._
import org.sireum.pilar.symbol.ProcedureSymbolTable
import org.sireum.util._
import org.sireum.pilar.ast._
import org.jgrapht.ext.VertexNameProvider
import java.io._
import org.jgrapht.ext.DOTExporter
import dk.brics.automaton._
import org.sireum.jawa.alir.compressedControlFlowGraph.AlirIntraProceduralGraphExtra
import org.sireum.jawa.alir.interProcedural.InterProceduralGraph
import org.sireum.jawa.alir.interProcedural.InterProceduralNode
import org.sireum.jawa.alir.Context
import scala.collection.immutable.BitSet
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.HashMap
import org.jgrapht.alg.DijkstraShortestPath
import org.sireum.jawa.alir.JawaAlirInfoProvider
import org.sireum.jawa.GlobalConfig
import org.sireum.jawa.Center
import org.sireum.jawa.alir.interProcedural.Callee
import org.sireum.jawa.JawaProcedure
import org.sireum.jawa.JawaCodeSource
import java.util.regex.Pattern

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
class InterproceduralControlFlowGraph[Node <: CGNode] extends InterProceduralGraph[Node]{
  private var succBranchMap : MMap[(Node, Option[Branch]), Node] = null
  private var predBranchMap : MMap[(Node, Option[Branch]), Node] = null
  val BRANCH_PROPERTY_KEY = ControlFlowGraph.BRANCH_PROPERTY_KEY
  final val EDGE_TYPE = "EdgeType"
  
  def addEdge(source : Node, target : Node, typ : String) : Edge = {
    val e = addEdge(source, target)
    if(typ != null)
    	e.setProperty(EDGE_TYPE, typ)
    e
  }
  
  def isEdgeType(e : Edge, typ : String) : Boolean = {
    e.getPropertyOrElse[String](EDGE_TYPE, null) == typ
  }
    
  protected var entryN : CGNode = null

  protected var exitN : CGNode = null
  
  val centerContext = new Context(GlobalConfig.CG_CONTEXT_K)
  centerContext.setContext("Center", "L0000")
  private val cNode = addCGCenterNode(centerContext)
  cNode.setOwner(Center.CENTER_PROCEDURE_SIG)
  protected var centerN : CGNode = cNode
  
  def centerNode : Node = this.centerN.asInstanceOf[Node]
  
  def entryNode : Node = this.entryN.asInstanceOf[Node]
  
  def exitNode : Node = this.exitN.asInstanceOf[Node]
  
  
  private val processed : MMap[(String, Context), ISet[Node]] = new HashMap[(String, Context), ISet[Node]] with SynchronizedMap[(String, Context), ISet[Node]]
  
  def isProcessed(proc : String, callerContext : Context) : Boolean = processed.contains(proc, callerContext)
  
  def addProcessed(jp : String, c : Context, nodes : ISet[Node]) = {
    this.processed += ((jp, c) -> nodes)
  }
  
  def getProcessed = this.processed
  
  def entryNode(proc : String, callerContext : Context) : Node = {
    require(isProcessed(proc, callerContext))
    processed(proc, callerContext).foreach{
      n => if(n.isInstanceOf[CGEntryNode]) return n
    }
    throw new RuntimeException("Cannot find entry node for: " + proc)
  }
  
  /**
   * map from procedures to it's callee procedures
   * map from caller sig to callee sigs
   */
  private var callMap : IMap[String, ISet[String]] = imapEmpty
  
  def setCallMap(from : String, to : String) = this.callMap += (from -> (this.callMap.getOrElse(from, isetEmpty) + to))
  
  def getCallMap = this.callMap

  def getReachableProcedures(procs : Set[String]) : Set[String] = {
    calculateReachableProcedures(procs, isetEmpty) ++ procs
  }
  
  private def calculateReachableProcedures(procs : Set[String], processed : Set[String]) : Set[String] = {
    if(procs.isEmpty) Set()
    else
      procs.map{
	      proc =>
	        if(processed.contains(proc)){
	          Set[String]()
	        } else {
		        val callees = this.callMap.getOrElse(proc, isetEmpty)
		        callees ++ calculateReachableProcedures(callees, processed + proc)
	        }
	    }.reduce((s1, s2) => s1 ++ s2)
  }
  
//  def getBackwardCallChains(procSig : String) : Set[Seq[String]] = {
//    if(procSig.isEmpty() || procSig == null) Set()
//    else {
//      val chains : MSet[Seq[String]] = msetEmpty
//      chains += Seq(procSig)
//      
//      val callers = getAllCaller(procSig)
//      calculateBackwardCallChains(callers, chains)
//      chains.toSet
//    }
//  }
//  
//  private def calculateBackwardCallChains(callers : Set[String], chains : MSet[Seq[String]]) : Boolean = {
//    var validCaller : Set[String] = Set() 
//    chains.foreach{
//      chain =>
//        callers.foreach{
//          caller =>
//            if(!chain.contains(caller)){
//              validCaller += caller
//              chains += chain :+ caller
//            }
//        }
//    }
//    if(validCaller.isEmpty) false
//    else {
//      validCaller.foreach{
//        caller =>
//          val newCallers = getAllCaller(caller)
//          calculateBackwardCallChains(newCallers, chains)
//      }
//    }
//  }
//  
//  private def getAllCaller(procSig : String) : Set[String] = {
//    var result : Set[String] = Set()
//    this.callMap.foreach{
//      case (caller, callees) =>
//        if(callees.contains(procSig)){
//          result += caller
//        } 
//    }
//    result
//  }
  
  def reverse : InterproceduralControlFlowGraph[Node] = {
    val result = new InterproceduralControlFlowGraph[Node]
    for (n <- nodes) result.addNode(n)
    for (e <- edges) result.addEdge(e.target, e.source)
    result.entryN = this.exitNode
    result.exitN = this.entryNode
    result.centerN = this.centerN
    result
  }
  
//  def merge(cg : CallGraph[Node]) = {
//    this.pl ++= cg.pool
//    cg.nodes.foreach(n => addNode(n))
//    cg.edges.foreach(e => addEdge(e))
//    this.processed ++= cg.getProcessed
//  }
    
  private def putBranchOnEdge(trans : Int, branch : Int, e : Edge) = {
    e(BRANCH_PROPERTY_KEY) = (trans, branch)
  }

  private def getBranch(pst : ProcedureSymbolTable, e : Edge) : Option[Branch] = {
    if (e ? BRANCH_PROPERTY_KEY) {
      val p : (Int, Int) = e(BRANCH_PROPERTY_KEY)
      var j =
        PilarAstUtil.getJumps(pst.location(
          e.source.asInstanceOf[AlirLocationNode].locIndex))(first2(p)).get
      val i = second2(p)

      if (j.isInstanceOf[CallJump])
        j = j.asInstanceOf[CallJump].jump.get

      (j : @unchecked) match {
        case gj : GotoJump   => Some(gj)
        case rj : ReturnJump => Some(rj)
        case ifj : IfJump =>
          if (i == 0) ifj.ifElse
          else Some(ifj.ifThens(i - 1))
        case sj : SwitchJump =>
          if (i == 0) sj.defaultCase
          else (Some(sj.cases(i - 1)))
      }
    } else None
  }

	def useBranch[T](pst : ProcedureSymbolTable)(f : => T) : T = {
	  succBranchMap = mmapEmpty
	  predBranchMap = mmapEmpty
	  for (node <- this.nodes) {
	    for (succEdge <- successorEdges(node)) {
	      val b = getBranch(pst, succEdge)
	      val s = edgeSource(succEdge)
	      val t = edgeTarget(succEdge)
	      succBranchMap((node, b)) = t
	      predBranchMap((t, b)) = s
	    }
	  }
	  val result = f
	  succBranchMap = null
	  predBranchMap = null
	  result
	}
    
    
    def successor(node : Node, branch : Option[Branch]) : Node = {
      assert(succBranchMap != null,
        "The successor method needs useBranch as enclosing context")
      succBranchMap((node, branch))
    }

    def predecessor(node : Node, branch : Option[Branch]) : Node = {
      assert(predBranchMap != null,
        "The successor method needs useBranch as enclosing context")
      predBranchMap((node, branch))
    }

    override def toString = {
      val sb = new StringBuilder("system CFG\n")

      for (n <- nodes)
        for (m <- successors(n)) {
          for (e <- getEdges(n, m)) {
            val branch = if (e ? BRANCH_PROPERTY_KEY)
              e(BRANCH_PROPERTY_KEY).toString
            else ""
              sb.append("%s -> %s %s\n".format(n, m, branch))
          }
        }

      sb.append("\n")

      sb.toString
    }
  
  /**
   * (We ASSUME that predecessors ???? and successors of n are within the same procedure as of n)
   * So, this algorithm is only for an internal node of a procedure NOT for a procedure's Entry node or Exit node
   * The algorithm is obvious from the following code 
   */
  def compressByDelNode (n : Node) = {
    val preds = predecessors(n) - n
    val succs = successors(n) - n
    deleteNode(n)
    for(pred <- preds){
      for(succ <- succs){           
        if (!hasEdge(pred,succ)){
          addEdge(pred, succ)
        }
      }
    }
  }
   
   // read the sCfg and build a corresponding DFA/NFA
  
   def buildAutomata() : Automaton = {
    val automata = new Automaton()
    // build a map between sCfg-nodes-set and automata-nodes-set
    val nodeMap:MMap[Node, State] = mmapEmpty
    
    nodes.foreach(
        gNode => {
          val state = new State()
          state.setAccept(true)  // making each state in the automata an accept state
          nodeMap(gNode) = state
        }
    )
    // build a map between Entry-nodes-set (in sCfg) to English characters (assuming Entry-nodes-set is small); note that each call corresponds to an edge to Entry node of callee proc
    val calleeMap:MMap[Node, Char] = mmapEmpty
    var calleeIndex = 0
    nodes.foreach(
        gNode => {   // ******* below check the hard-coded path for testing ***********
          if(!gNode.toString().contains("pilar:/procedure/default/%5B%7Cde::mobinauten") && gNode.toString().endsWith(".Entry"))
          {
            calleeMap(gNode) = ('A' + calleeIndex).toChar
            println("in calleeMap: node " + gNode.toString() + "  has label = " + calleeMap(gNode))
            calleeIndex = calleeIndex + 1
          }
        }
    )
    // build the automata from the sCfg
    
    nodes.foreach(
        gNode => {
          val automataNode = nodeMap(gNode)   // automataNode = automata state
          val gSuccs = successors(gNode)
          var label: Char = 'x'  // default label of automata transition
          gSuccs.foreach(
              gSucc => {
                val automataSucc = nodeMap(gSucc)
                if(calleeMap.contains(gSucc))
                  label = calleeMap(gSucc)
                val tr = new Transition(label, automataSucc)
                automataNode.addTransition(tr)
              }
          )
        }
    )
    // get start node S from sCfg by searching with relevant pUri and then get corresponding automata state
    nodes.foreach(
        gNode => { // ******* below check the hard-coded path for testing ***********
		   if(gNode.toString().contains("pilar:/procedure/default/%5B%7Cde::mobinauten::smsspy::EmergencyTask.onLocationChanged%7C%5D/1/23/51eae215") && gNode.toString().endsWith(".Entry")) 
		   {
			   // val gStartNode = getVirtualNode("pilar:/procedure/default/%5B%7Cde::mobinauten::smsspy::EmergencyTask.onLocationChanged%7C%5D/1/23/51eae215.Entry".asInstanceOf[VirtualLabel]
			   val startState = nodeMap(gNode)
			   automata.setInitialState(startState)
			   println(automata.toDot())
		   }
		}       
    )
   automata
  }
   
  def isCall(l : LocationDecl) : Boolean = l.isInstanceOf[JumpLocation] && l.asInstanceOf[JumpLocation].jump.isInstanceOf[CallJump]
   
  def merge(cg : InterproceduralControlFlowGraph[Node]) = {
    this.pl ++= cg.pool
    cg.nodes.foreach(addNode(_))
    cg.edges.foreach(addEdge(_))
    cg.callMap.foreach{
      case (src, dsts) =>
        this.callMap += (src -> (this.callMap.getOrElse(src, isetEmpty) ++ dsts))
    }
    this.processed ++= cg.processed
    this.predBranchMap ++= cg.predBranchMap
    this.succBranchMap ++= cg.succBranchMap
  }
  
  def collectCfgToBaseGraph[VirtualLabel](calleeProc : JawaProcedure, callerContext : Context, isFirst : Boolean = false) = {
    this.synchronized{
	    val calleeSig = calleeProc.getSignature
	    if(!calleeProc.checkLevel(Center.ResolveLevel.BODY)) calleeProc.resolveBody
	    val body = calleeProc.getProcedureBody
	    val rawcode = JawaCodeSource.getProcedureCodeWithoutFailing(calleeProc.getSignature)
	    val codes = rawcode.split("\\r?\\n")
	    val cfg = JawaAlirInfoProvider.getCfg(calleeProc)
	    var nodes = isetEmpty[Node]
	    cfg.nodes map{
	      n =>
		      n match{
		        case vn : AlirVirtualNode[VirtualLabel] =>
		          vn.label.toString match{
		            case "Entry" => 
		              val entryNode = addCGEntryNode(callerContext.copy.setContext(calleeSig, "Entry"))
		              entryNode.setOwner(calleeProc.getSignature)
		              nodes += entryNode
		              if(isFirst) this.entryN = entryNode
		            case "Exit" => 
		              val exitNode = addCGExitNode(callerContext.copy.setContext(calleeSig,  "Exit"))
		              exitNode.setOwner(calleeProc.getSignature)
		              nodes += exitNode
		              if(isFirst) this.exitN = exitNode
		            case a => throw new RuntimeException("unexpected virtual label: " + a)
		          }
		        case ln : AlirLocationUriNode=>
		          val l = body.location(ln.locIndex)
		          val code = codes.find(_.contains("#" + ln.locUri + ".")).getOrElse(throw new RuntimeException("Could not find " + ln.locUri + " from \n" + rawcode))
		          if(isCall(l)){
	              val c = addCGCallNode(callerContext.copy.setContext(calleeSig, ln.locUri))
	              c.setOwner(calleeProc.getSignature)
	              c.setCode(code)
	              c.asInstanceOf[CGLocNode].setLocIndex(ln.locIndex)
	              nodes += c
	              val r = addCGReturnNode(callerContext.copy.setContext(calleeSig, ln.locUri))
	              r.setOwner(calleeProc.getSignature)
	              r.setCode(code)
	              r.asInstanceOf[CGLocNode].setLocIndex(ln.locIndex)
	              nodes += r
	//              addEdge(c, r)
		          } else {
		            val node = addCGNormalNode(callerContext.copy.setContext(calleeSig, ln.locUri))
		            node.setOwner(calleeProc.getSignature)
		            node.setCode(code)
		            node.asInstanceOf[CGLocNode].setLocIndex(ln.locIndex)
		            nodes += node
		          }
		        case a : AlirLocationNode => 
		          // should not have a chance to reach here.
		          val node = addCGNormalNode(callerContext.copy.setContext(calleeSig, a.locIndex.toString))
		          node.setOwner(calleeProc.getSignature)
		          node.setCode("unknown")
		          node.asInstanceOf[CGLocNode].setLocIndex(a.locIndex)
		          nodes += node
		      }
	    }
	    for (e <- cfg.edges) {
	      val entryNode = getCGEntryNode(callerContext.copy.setContext(calleeSig, "Entry"))
	      val exitNode = getCGExitNode(callerContext.copy.setContext(calleeSig, "Exit"))
	      e.source match{
	        case vns : AlirVirtualNode[VirtualLabel] =>
	          e.target match{
	            case vnt : AlirVirtualNode[VirtualLabel] =>
	              addEdge(entryNode, exitNode)
	            case lnt : AlirLocationUriNode =>
	              val lt = body.location(lnt.locIndex)
			          if(isCall(lt)){
	                val callNodeTarget = getCGCallNode(callerContext.copy.setContext(calleeSig, lnt.locUri))
	                addEdge(entryNode, callNodeTarget)
			          } else {
		              val targetNode = getCGNormalNode(callerContext.copy.setContext(calleeSig, lnt.locUri))
		              addEdge(entryNode, targetNode)
			          }
	            case nt =>
	              val targetNode = getCGNormalNode(callerContext.copy.setContext(calleeSig, nt.toString))
	              addEdge(entryNode, targetNode)
	          }
	        case lns : AlirLocationUriNode =>
	          val ls = body.location(lns.locIndex)
	          e.target match{
	            case vnt : AlirVirtualNode[VirtualLabel] =>
	              if(isCall(ls)){
	                val returnNodeSource = getCGReturnNode(callerContext.copy.setContext(calleeSig, lns.locUri))
	                addEdge(returnNodeSource, exitNode)
	              } else {
	                val sourceNode = getCGNormalNode(callerContext.copy.setContext(calleeSig, lns.locUri))
	              	addEdge(sourceNode, exitNode)
	              }
	            case lnt : AlirLocationUriNode =>
	              val lt = body.location(lnt.locIndex)
	              if(isCall(ls)){
	                val returnNodeSource = getCGReturnNode(callerContext.copy.setContext(calleeSig, lns.locUri))
	                if(isCall(lt)){
		                val callNodeTarget = getCGCallNode(callerContext.copy.setContext(calleeSig, lnt.locUri))
		                addEdge(returnNodeSource, callNodeTarget)
				          } else {
			              val targetNode = getCGNormalNode(callerContext.copy.setContext(calleeSig, lnt.locUri))
			              addEdge(returnNodeSource, targetNode)
				          }
	              } else {
	                val sourceNode = getCGNormalNode(callerContext.copy.setContext(calleeSig, lns.locUri))
	                if(isCall(lt)){
		                val callNodeTarget = getCGCallNode(callerContext.copy.setContext(calleeSig, lnt.locUri))
		                addEdge(sourceNode, callNodeTarget)
				          } else {
			              val targetNode = getCGNormalNode(callerContext.copy.setContext(calleeSig, lnt.locUri))
			              addEdge(sourceNode, targetNode)
				          }
	              }
	            case nt =>
	              val targetNode = getCGNormalNode(callerContext.copy.setContext(calleeSig, nt.toString))
	              if(isCall(ls)){
	                val returnNodeSource = getCGReturnNode(callerContext.copy.setContext(calleeSig, lns.locUri))
	                addEdge(returnNodeSource, targetNode)
	              } else {
	                val sourceNode = getCGNormalNode(callerContext.copy.setContext(calleeSig, lns.locUri))
	                addEdge(sourceNode, targetNode)
	              }
	          }
	        case ns =>
	          val sourceNode = getCGNormalNode(callerContext.copy.setContext(calleeSig, ns.toString))
	          e.target match{
	            case vnt : AlirVirtualNode[VirtualLabel] =>
	              addEdge(sourceNode, exitNode)
	            case lnt : AlirLocationUriNode =>
	              val lt = body.location(lnt.locIndex)
			          if(isCall(lt)){
	                val callNodeTarget = getCGCallNode(callerContext.copy.setContext(calleeSig, lnt.locUri))
	                val returnNodeTarget = getCGReturnNode(callerContext.copy.setContext(calleeSig, lnt.locUri))
	                addEdge(sourceNode, callNodeTarget)
			          } else {
		              val targetNode = getCGNormalNode(callerContext.copy.setContext(calleeSig, lnt.locUri))
		              addEdge(sourceNode, targetNode)
			          }
	            case nt =>
	              val targetNode = getCGNormalNode(callerContext.copy.setContext(calleeSig, nt.toString))
	              addEdge(sourceNode, targetNode)
	          }
	      }
	    }
	    addProcessed(calleeProc.getSignature, callerContext, nodes)
	    nodes
    }
  }
  
  def extendGraph(calleeSig  : String, callerContext : Context) : Node = {
    val callNode = getCGCallNode(callerContext)
    val returnNode = getCGReturnNode(callerContext)
    val calleeEntryContext = callerContext.copy
    calleeEntryContext.setContext(calleeSig, "Entry")
    val calleeExitContext = callerContext.copy
    calleeExitContext.setContext(calleeSig, "Exit")
    val targetNode = getCGEntryNode(calleeEntryContext)
    val retSrcNode = getCGExitNode(calleeExitContext)
    this.synchronized{
      setCallMap(callNode.getOwner, targetNode.getOwner)
      if(!hasEdge(callNode, targetNode))
      	addEdge(callNode, targetNode)
      if(!hasEdge(retSrcNode, returnNode))
      	addEdge(retSrcNode, returnNode)
    }
    targetNode
  }
  
  def extendGraphOneWay(calleeSig  : String, callerContext : Context, typ : String = null) : Node = {
    val callNode = getCGCallNode(callerContext)
    val calleeEntryContext = callerContext.copy
    calleeEntryContext.setContext(calleeSig, "Entry")
    val targetNode = getCGEntryNode(calleeEntryContext)
    this.synchronized{
      setCallMap(callNode.getOwner, targetNode.getOwner)
      if(!hasEdge(callNode, targetNode))
        addEdge(callNode, targetNode, typ)
    }
    targetNode
  }
  
  def toCallGraph : InterproceduralControlFlowGraph[Node] = {
    val ns = nodes filter{
      n =>
        n match{
          case cn : CGCallNode =>
            false
          case _ => true
        }
    }
    ns foreach(compressByDelNode(_))
    this
  }
  
  def toApiGraph : InterproceduralControlFlowGraph[Node] = {
    val ns = nodes filter{
      n =>
        n match{
          case cn : CGCallNode =>
            cn.getCalleeSet.exists { c => !c.callee.getDeclaringRecord.isFrameworkRecord }
          case _ => true
        }
    }
    ns foreach(compressByDelNode(_))
    this
  }
  
  private def getSignatureFromCallNode(node : Node) : String = {
    assume(node.isInstanceOf[CGCallNode])
    val loc = Center.getProcedureWithoutFailing(node.getOwner).getProcedureBody.location(node.asInstanceOf[CGCallNode].getLocIndex)
    val sig = loc.asInstanceOf[JumpLocation].jump.getValueAnnotation("signature") match {
      case Some(s) => s match {
        case ne : NameExp => ne.name.name
        case _ => ""
      }
      case None => throw new RuntimeException("cannot found annotation 'signature' from: " + loc)
    }
    sig
  }
  
  def toTextGraph(w : Writer)  = {
    var res : String = ""
    res += "Nodes:\n"
    nodes.foreach{
      node =>
        val nStr = node.getContext.toFullString + " ::::> " + node.getCode
        res += nStr + "\n"
    }
    res += "Edges:\n"
    edges.foreach{
      edge =>
        val eStr = edge.source.getContext.toFullString + " --> " + edge.target.getContext.toFullString
        res += eStr + "\n"
    }
    w.write(res)
  }
  
  def addCGNormalNode(context : Context) : Node = {
    val node = newCGNormalNode(context).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }
  
  def cgNormalNodeExists(context : Context) : Boolean = {
    graph.containsVertex(newCGNormalNode(context).asInstanceOf[Node])
  }
  
  def getCGNormalNode(context : Context) : Node =
    pool(newCGNormalNode(context))
  
  protected def newCGNormalNode(context : Context) =
    CGNormalNode(context)
    
  def addCGCallNode(context : Context) : Node = {
    val node = newCGCallNode(context).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }
  
  def cgCallNodeExists(context : Context) : Boolean = {
    graph.containsVertex(newCGCallNode(context).asInstanceOf[Node])
  }
  
  def getCGCallNode(context : Context) : Node =
    pool(newCGCallNode(context))
  
  protected def newCGCallNode(context : Context) =
    CGCallNode(context)
    
  def addCGReturnNode(context : Context) : Node = {
    val node = newCGReturnNode(context).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }
  
  def cgReturnNodeExists(context : Context) : Boolean = {
    graph.containsVertex(newCGReturnNode(context).asInstanceOf[Node])
  }
  
  def getCGReturnNode(context : Context) : Node =
    pool(newCGReturnNode(context))
  
  protected def newCGReturnNode(context : Context) =
    CGReturnNode(context)
  
    
  def addCGEntryNode(context : Context) : Node = {
    val node = newCGEntryNode(context).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }
  
  def cgEntryNodeExists(context : Context) : Boolean = {
    graph.containsVertex(newCGEntryNode(context).asInstanceOf[Node])
  }
  
  def getCGEntryNode(context : Context) : Node =
    pool(newCGEntryNode(context))
  
  protected def newCGEntryNode(context : Context) =
    CGEntryNode(context)
    
  def addCGCenterNode(context : Context) : Node = {
    val node = newCGCenterNode(context).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }
  
  def cgCGCenterNodeExists(context : Context) : Boolean = {
    graph.containsVertex(newCGCenterNode(context).asInstanceOf[Node])
  }
  
  def getCGCenterNode(context : Context) : Node =
    pool(newCGCenterNode(context))
  
  protected def newCGCenterNode(context : Context) =
    CGCenterNode(context)
    
  def addCGExitNode(context : Context) : Node = {
    val node = newCGExitNode(context).asInstanceOf[Node]
    val n =
      if (pool.contains(node)) pool(node)
      else {
        pl += (node -> node)
        node
      }
    graph.addVertex(n)
    n
  }
  
  def cgExitNodeExists(context : Context) : Boolean = {
    graph.containsVertex(newCGExitNode(context).asInstanceOf[Node])
  }
  
  def getCGExitNode(context : Context) : Node =
    pool(newCGExitNode(context))
  
  protected def newCGExitNode(context : Context) =
    CGExitNode(context)
  
}

sealed abstract class CGNode(context : Context) extends InterProceduralNode(context){
  protected var owner : String = null
  protected var loadedClassBitSet : BitSet = BitSet.empty
  protected var code : String = null
  def setOwner(owner : String)  = this.owner = owner
  def getOwner = this.owner
  def setCode(code : String) = this.code = code
  def getCode : String = this.code
  def setLoadedClassBitSet(bitset : BitSet) = this.loadedClassBitSet = bitset
  def getLoadedClassBitSet = this.loadedClassBitSet
  
//  def updateLoadedClassBitSet(bitset : BitSet) = {
//    if(getLoadedClassBitSet == BitSet.empty) setLoadedClassBitSet(bitset)
//    else setLoadedClassBitSet(bitset.intersect(getLoadedClassBitSet))
//  }
}

abstract class CGVirtualNode(context : Context) extends CGNode(context) {
  def getVirtualLabel : String
  
  override def toString : String = getVirtualLabel + "@" + context
}

final case class CGEntryNode(context : Context) extends CGVirtualNode(context){
  this.code = "Entry: " + context.getProcedureSig
  def getVirtualLabel : String = "Entry"
}

final case class CGExitNode(context : Context) extends CGVirtualNode(context){
  this.code = "Exit: " + context.getProcedureSig
  def getVirtualLabel : String = "Exit"
}

final case class CGCenterNode(context : Context) extends CGVirtualNode(context){
  this.code = "L0000: Center;"
  def getVirtualLabel : String = "Center"
}

abstract class CGLocNode(context : Context) extends CGNode(context) {
  def getLocUri : String = context.getLocUri
  protected val LOC_INDEX = "LocIndex"
  def setLocIndex(i : Int) = setProperty(LOC_INDEX, i)
  def getLocIndex : Int = getPropertyOrElse[Int](LOC_INDEX, throw new RuntimeException("did not have loc index"))
}

abstract class CGInvokeNode(context : Context) extends CGLocNode(context) {
  final val CALLEES = "callee_set"
  def getInvokeLabel : String
  def setCalleeSet(calleeSet : ISet[Callee]) = this.setProperty(CALLEES, calleeSet)
  def getCalleeSet : ISet[Callee] = this.getPropertyOrElse(CALLEES, isetEmpty)
  override def toString : String = getInvokeLabel + "@" + context
}

final case class CGCallNode(context : Context) extends CGInvokeNode(context){
  def getInvokeLabel : String = "Call"
}

final case class CGReturnNode(context : Context) extends CGInvokeNode(context){
  def getInvokeLabel : String = "Return"
}

final case class CGNormalNode(context : Context) extends CGLocNode(context){
  override def toString : String = context.toString
}
