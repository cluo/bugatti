package controllers.admin

import controllers.BaseController
import models.conf._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Action

/**
 * Created by jinwei on 9/8/14.
 */
object DependencyController extends BaseController{

  def show(id: Int) = Action{
    val project = ProjectHelper.findById(id).get
    val subProjects = ProjectHelper.findDependencyProjects(id)
    val tdMaps = ProjectDependencyHelper.findByProjectId(id).filter(pd => pd.alias != None).map(t => t.dependencyId -> t.alias.get).toMap
    val dependencyNests = subProjects.map{
      s =>
        if(tdMaps.keySet.contains(s.id.get)){
          DependencyNest(s.id.get, s.name, false, tdMaps.get(s.id.get).get, s.templateId, Seq.empty[DependencyNest])
        }else {
          DependencyNest(s.id.get, s.name, true, "", s.templateId, Seq.empty[DependencyNest])
        }
    }
    val result = Seq(
      DependencyNest(id, project.name, false, "", project.templateId, dependencyNests)
    ).sortBy(t => t.id)
    Ok(Json.toJson(result))
  }

  def removeDependency(parentId: Int, childId: Int) = AuthAction() {
    Ok(Json.toJson(ProjectDependencyHelper.removeByP_C(parentId, childId)))
  }

  def addDependency() = AuthAction() { implicit request =>

    def add(fields: Seq[(String, JsValue)]): Int = {
      val fieldsJson = Json.toJson(fields.toMap)
      val p = (fieldsJson \ "parent").as[DependencyNest]
      val c = (fieldsJson \ "child").as[Project]
      try{
        ProjectDependencyHelper.addByP_C(p, c)
      } catch {
        case e: Exception => 0
      }
    }

    request.body.asJson match {
      case Some(JsObject(fields)) => {
        Ok(Json.obj("r" -> add(fields)))
      }
      case _ => Ok(Json.obj("r" -> 0))
    }
  }

  def updateTemplateProject() = AuthAction() { implicit request =>

    def update(fields: Seq[(String, JsValue)]): Int = {
      val fieldsJson = Json.toJson(fields.toMap)
      val p = (fieldsJson \ "parentId").as[Int]
      val o = (fieldsJson \ "oldId").as[Int]
      val n = (fieldsJson \ "newId").as[Int]

      try{
        ProjectDependencyHelper.updateByP_C(p, o, n)
      } catch {
        case e: Exception => 0
      }
    }

    request.body.asJson match {
      case Some(JsObject(fields)) => {
        Ok(Json.obj("r" -> update(fields)))
      }
      case _ => Ok(Json.obj("r" -> 0))
    }
  }


  implicit val dnWrites = Json.writes[DependencyNest]
  implicit val dnReads = Json.reads[DependencyNest]
  implicit val variableReads = Json.reads[Variable]
  implicit val projectReads = Json.reads[Project]
}


case class DependencyNest(id: Int, name: String, canRemove: Boolean , alias: String, templateId: Int, dependency: Seq[DependencyNest])
