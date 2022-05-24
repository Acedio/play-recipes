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
class HomeController @Inject()(recipeService: models.RecipeRepository)(val controllerComponents: ControllerComponents)
    extends BaseController {
  val logger = Logger(this.getClass())

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  // TODO: Should we define our own EC instead?
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  // TODO: timestamps should be DateTimes.
  implicit val recipeReads = Json.reads[models.Recipe]
  implicit val recipeWrites = Json.writes[models.Recipe]

  def validateJson[A: Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  def createRecipe() = Action(validateJson[models.Recipe]) { request =>
    Ok("Got: " + request.body.id)
  }

  def listRecipes() = Action {
    Ok("these are recipes!")
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
