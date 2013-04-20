// Do not edit this file. It is auto-generated from org.sireum.amandroid.module.Cfg
// by org.sireum.pipeline.gen.ModuleGenerator

package org.sireum.amandroid.module

import org.sireum.util._
import org.sireum.pipeline._
import java.lang.String
import org.sireum.alir.AlirIntraProceduralNode
import org.sireum.alir.ControlFlowGraph
import org.sireum.amandroid.AndroidSymbolResolver.AndroidVirtualMethodTables
import org.sireum.amandroid.cache.AndroidCacheFile
import org.sireum.pilar.ast.LocationDecl
import org.sireum.pilar.symbol.ProcedureSymbolTable
import org.sireum.pilar.symbol.SymbolTable
import scala.Function2
import scala.Option
import scala.collection.mutable.Map

object CfgModule extends PipelineModule {
  def title = "Control Flow Graph Builder"
  def origin = classOf[Cfg]

  val poolKey = "Cfg.pool"
  val globalProcedureSymbolTableKey = "Global.procedureSymbolTable"
  val globalPoolKey = "Global.pool"
  val cfgKey = "Cfg.cfg"
  val globalShouldIncludeFlowFunctionKey = "Global.shouldIncludeFlowFunction"
  val globalCfgKey = "Global.cfg"

  def compute(job : PipelineJob, info : PipelineJobModuleInfo) : MBuffer[Tag] = {
    val tags = marrayEmpty[Tag]
    try {
      val module = Class.forName("org.sireum.amandroid.module.CfgModuleDef")
      val cons = module.getConstructors()(0)
      val params = Array[AnyRef](job, info)
      val inst = cons.newInstance(params : _*)
    } catch {
      case e : Throwable =>
        e.printStackTrace
        tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker, e.getMessage);
    }
    return tags
  }

  override def initialize(job : PipelineJob) {
  }

  override def validPipeline(stage : PipelineStage, job : PipelineJob) : MBuffer[Tag] = {
    val tags = marrayEmpty[Tag]
    val deps = ilist[PipelineModule]()
    deps.foreach(d =>
      if(stage.modules.contains(d)){
        tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "'" + this.title + "' depends on '" + d.title + "' yet both were found in stage '" + stage.title + "'"
        )
      }
    )
    return tags
  }

  def inputDefined (job : PipelineJob) : MBuffer[Tag] = {
    val tags = marrayEmpty[Tag]
    var _procedureSymbolTable : scala.Option[AnyRef] = None
    var _procedureSymbolTableKey : scala.Option[String] = None

    val keylistprocedureSymbolTable = List(CfgModule.globalProcedureSymbolTableKey)
    keylistprocedureSymbolTable.foreach(key => 
      if(job ? key) { 
        if(_procedureSymbolTable.isEmpty) {
          _procedureSymbolTable = Some(job(key))
          _procedureSymbolTableKey = Some(key)
        }
        if(!(job(key).asInstanceOf[AnyRef] eq _procedureSymbolTable.get)) {
          tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "Input error for '" + this.title + "': 'procedureSymbolTable' keys '" + _procedureSymbolTableKey.get + " and '" + key + "' point to different objects.")
        }
      }
    )

    _procedureSymbolTable match{
      case Some(x) =>
        if(!x.isInstanceOf[org.sireum.pilar.symbol.ProcedureSymbolTable]){
          tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "Input error for '" + this.title + "': Wrong type found for 'procedureSymbolTable'.  Expecting 'org.sireum.pilar.symbol.ProcedureSymbolTable' but found '" + x.getClass.toString + "'")
        }
      case None =>
        tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
          "Input error for '" + this.title + "': No value found for 'procedureSymbolTable'")       
    }
    var _shouldIncludeFlowFunction : scala.Option[AnyRef] = None
    var _shouldIncludeFlowFunctionKey : scala.Option[String] = None

    val keylistshouldIncludeFlowFunction = List(CfgModule.globalShouldIncludeFlowFunctionKey)
    keylistshouldIncludeFlowFunction.foreach(key => 
      if(job ? key) { 
        if(_shouldIncludeFlowFunction.isEmpty) {
          _shouldIncludeFlowFunction = Some(job(key))
          _shouldIncludeFlowFunctionKey = Some(key)
        }
        if(!(job(key).asInstanceOf[AnyRef] eq _shouldIncludeFlowFunction.get)) {
          tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "Input error for '" + this.title + "': 'shouldIncludeFlowFunction' keys '" + _shouldIncludeFlowFunctionKey.get + " and '" + key + "' point to different objects.")
        }
      }
    )

    _shouldIncludeFlowFunction match{
      case Some(x) =>
        if(!x.isInstanceOf[scala.Function2[org.sireum.pilar.ast.LocationDecl, scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Tuple2[scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Boolean]]]){
          tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
            "Input error for '" + this.title + "': Wrong type found for 'shouldIncludeFlowFunction'.  Expecting 'scala.Function2[org.sireum.pilar.ast.LocationDecl, scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Tuple2[scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Boolean]]' but found '" + x.getClass.toString + "'")
        }
      case None =>
        tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
          "Input error for '" + this.title + "': No value found for 'shouldIncludeFlowFunction'")       
    }
    return tags
  }

  def outputDefined (job : PipelineJob) : MBuffer[Tag] = {
    val tags = marrayEmpty[Tag]
    if(!(job ? CfgModule.poolKey) && !(job ? CfgModule.globalPoolKey)) {
      tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
        "Output error for '" + this.title + "': No entry found for 'pool'. Expecting (CfgModule.poolKey or CfgModule.globalPoolKey)") 
    }

    if(job ? CfgModule.poolKey && !job(CfgModule.poolKey).isInstanceOf[scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]]) {
      tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker, 
        "Output error for '" + this.title + "': Wrong type found for CfgModule.poolKey.  Expecting 'scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]' but found '" + 
        job(CfgModule.poolKey).getClass.toString + "'")
    } 

    if(job ? CfgModule.globalPoolKey && !job(CfgModule.globalPoolKey).isInstanceOf[scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]]) {
      tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker, 
        "Output error for '" + this.title + "': Wrong type found for CfgModule.globalPoolKey.  Expecting 'scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]' but found '" + 
        job(CfgModule.globalPoolKey).getClass.toString + "'")
    } 

    if(!(job ? CfgModule.cfgKey) && !(job ? CfgModule.globalCfgKey)) {
      tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker,
        "Output error for '" + this.title + "': No entry found for 'cfg'. Expecting (CfgModule.cfgKey or CfgModule.globalCfgKey)") 
    }

    if(job ? CfgModule.cfgKey && !job(CfgModule.cfgKey).isInstanceOf[org.sireum.alir.ControlFlowGraph[java.lang.String]]) {
      tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker, 
        "Output error for '" + this.title + "': Wrong type found for CfgModule.cfgKey.  Expecting 'org.sireum.alir.ControlFlowGraph[java.lang.String]' but found '" + 
        job(CfgModule.cfgKey).getClass.toString + "'")
    } 

    if(job ? CfgModule.globalCfgKey && !job(CfgModule.globalCfgKey).isInstanceOf[org.sireum.alir.ControlFlowGraph[java.lang.String]]) {
      tags += PipelineUtil.genTag(PipelineUtil.ErrorMarker, 
        "Output error for '" + this.title + "': Wrong type found for CfgModule.globalCfgKey.  Expecting 'org.sireum.alir.ControlFlowGraph[java.lang.String]' but found '" + 
        job(CfgModule.globalCfgKey).getClass.toString + "'")
    } 
    return tags
  }

  def getPool (options : scala.collection.Map[Property.Key, Any]) : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode] = {
    if (options.contains(CfgModule.globalPoolKey)) {
       return options(CfgModule.globalPoolKey).asInstanceOf[scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]]
    }
    if (options.contains(CfgModule.poolKey)) {
       return options(CfgModule.poolKey).asInstanceOf[scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]]
    }

    throw new Exception("Pipeline checker should guarantee we never reach here")
  }

  def setPool (options : MMap[Property.Key, Any], pool : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]) : MMap[Property.Key, Any] = {

    options(CfgModule.globalPoolKey) = pool
    options(poolKey) = pool

    return options
  }

  def getProcedureSymbolTable (options : scala.collection.Map[Property.Key, Any]) : org.sireum.pilar.symbol.ProcedureSymbolTable = {
    if (options.contains(CfgModule.globalProcedureSymbolTableKey)) {
       return options(CfgModule.globalProcedureSymbolTableKey).asInstanceOf[org.sireum.pilar.symbol.ProcedureSymbolTable]
    }

    throw new Exception("Pipeline checker should guarantee we never reach here")
  }

  def setProcedureSymbolTable (options : MMap[Property.Key, Any], procedureSymbolTable : org.sireum.pilar.symbol.ProcedureSymbolTable) : MMap[Property.Key, Any] = {

    options(CfgModule.globalProcedureSymbolTableKey) = procedureSymbolTable

    return options
  }

  def getShouldIncludeFlowFunction (options : scala.collection.Map[Property.Key, Any]) : scala.Function2[org.sireum.pilar.ast.LocationDecl, scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Tuple2[scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Boolean]] = {
    if (options.contains(CfgModule.globalShouldIncludeFlowFunctionKey)) {
       return options(CfgModule.globalShouldIncludeFlowFunctionKey).asInstanceOf[scala.Function2[org.sireum.pilar.ast.LocationDecl, scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Tuple2[scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Boolean]]]
    }

    throw new Exception("Pipeline checker should guarantee we never reach here")
  }

  def setShouldIncludeFlowFunction (options : MMap[Property.Key, Any], shouldIncludeFlowFunction : scala.Function2[org.sireum.pilar.ast.LocationDecl, scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Tuple2[scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Boolean]]) : MMap[Property.Key, Any] = {

    options(CfgModule.globalShouldIncludeFlowFunctionKey) = shouldIncludeFlowFunction

    return options
  }

  def getCfg (options : scala.collection.Map[Property.Key, Any]) : org.sireum.alir.ControlFlowGraph[java.lang.String] = {
    if (options.contains(CfgModule.globalCfgKey)) {
       return options(CfgModule.globalCfgKey).asInstanceOf[org.sireum.alir.ControlFlowGraph[java.lang.String]]
    }
    if (options.contains(CfgModule.cfgKey)) {
       return options(CfgModule.cfgKey).asInstanceOf[org.sireum.alir.ControlFlowGraph[java.lang.String]]
    }

    throw new Exception("Pipeline checker should guarantee we never reach here")
  }

  def setCfg (options : MMap[Property.Key, Any], cfg : org.sireum.alir.ControlFlowGraph[java.lang.String]) : MMap[Property.Key, Any] = {

    options(CfgModule.globalCfgKey) = cfg
    options(cfgKey) = cfg

    return options
  }

  object ConsumerView {
    implicit class CfgModuleConsumerView (val job : PropertyProvider) extends AnyVal {
      def pool : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode] = CfgModule.getPool(job.propertyMap)
      def procedureSymbolTable : org.sireum.pilar.symbol.ProcedureSymbolTable = CfgModule.getProcedureSymbolTable(job.propertyMap)
      def shouldIncludeFlowFunction : scala.Function2[org.sireum.pilar.ast.LocationDecl, scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Tuple2[scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Boolean]] = CfgModule.getShouldIncludeFlowFunction(job.propertyMap)
      def cfg : org.sireum.alir.ControlFlowGraph[java.lang.String] = CfgModule.getCfg(job.propertyMap)
    }
  }

  object ProducerView {
    implicit class CfgModuleProducerView (val job : PropertyProvider) extends AnyVal {

      def pool_=(pool : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]) { CfgModule.setPool(job.propertyMap, pool) }
      def pool : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode] = CfgModule.getPool(job.propertyMap)

      def procedureSymbolTable_=(procedureSymbolTable : org.sireum.pilar.symbol.ProcedureSymbolTable) { CfgModule.setProcedureSymbolTable(job.propertyMap, procedureSymbolTable) }
      def procedureSymbolTable : org.sireum.pilar.symbol.ProcedureSymbolTable = CfgModule.getProcedureSymbolTable(job.propertyMap)

      def shouldIncludeFlowFunction_=(shouldIncludeFlowFunction : scala.Function2[org.sireum.pilar.ast.LocationDecl, scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Tuple2[scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Boolean]]) { CfgModule.setShouldIncludeFlowFunction(job.propertyMap, shouldIncludeFlowFunction) }
      def shouldIncludeFlowFunction : scala.Function2[org.sireum.pilar.ast.LocationDecl, scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Tuple2[scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Boolean]] = CfgModule.getShouldIncludeFlowFunction(job.propertyMap)

      def cfg_=(cfg : org.sireum.alir.ControlFlowGraph[java.lang.String]) { CfgModule.setCfg(job.propertyMap, cfg) }
      def cfg : org.sireum.alir.ControlFlowGraph[java.lang.String] = CfgModule.getCfg(job.propertyMap)
    }
  }
}

trait CfgModule {
  def job : PipelineJob


  def pool_=(pool : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode]) { CfgModule.setPool(job.propertyMap, pool) }
  def pool : scala.collection.mutable.Map[org.sireum.alir.AlirIntraProceduralNode, org.sireum.alir.AlirIntraProceduralNode] = CfgModule.getPool(job.propertyMap)

  def procedureSymbolTable : org.sireum.pilar.symbol.ProcedureSymbolTable = CfgModule.getProcedureSymbolTable(job.propertyMap)

  def shouldIncludeFlowFunction : scala.Function2[org.sireum.pilar.ast.LocationDecl, scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Tuple2[scala.collection.Iterable[org.sireum.pilar.ast.CatchClause], scala.Boolean]] = CfgModule.getShouldIncludeFlowFunction(job.propertyMap)


  def cfg_=(cfg : org.sireum.alir.ControlFlowGraph[java.lang.String]) { CfgModule.setCfg(job.propertyMap, cfg) }
  def cfg : org.sireum.alir.ControlFlowGraph[java.lang.String] = CfgModule.getCfg(job.propertyMap)
}