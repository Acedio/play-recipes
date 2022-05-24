package controllers

import javax.inject._

import play.api._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class RecipesController @Inject()(recipeService: models.RecipeRepository)(val controllerComponents: ControllerComponents)
    extends BaseController {
  val logger = Logger(this.getClass())

  // TODO: Should we define our own EC instead?
  implicit val ec: ExecutionContext = ExecutionContext.global

  implicit val recipeReads = Json.reads[models.Recipe]
  implicit val recipeWrites = Json.writes[models.Recipe]

  def validateJson[A: Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  def createRecipe() = Action(validateJson[models.Recipe]) { request =>
    Ok("Got: " + request.body.id)
  }

  def listRecipes() = Action.async {
    recipeService.list().map(rs => Ok(Json.toJson(rs).toString()))
  }

  def getRecipe(id: Long) = Action.async {
    recipeService.get(id).map(_ match {
        case Some(r) => Ok(Json.toJson(r).toString())
        case None => NotFound
      }
    )
  }
  def updateRecipe(id: Long) = TODO
  def deleteRecipe(id: Long) = TODO
}
