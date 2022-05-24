package controllers

import javax.inject._

import play.api._
import play.api.libs.json._
import play.api.mvc._

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class RecipesController @Inject()(recipeService: models.RecipeRepository, 
                                  val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext)
    extends BaseController {
  val logger = Logger(this.getClass())

  implicit val dateTimeReads = new Format[DateTime] {
    def writes(dt: DateTime): JsValue =
      JsString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").print(dt))
    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsString(s) => 
        JsSuccess(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(s))
      case js => JsError(
        Seq(
          JsPath ->
            Seq(JsonValidationError("error.expected.date.isoformat", js))
        )
      )
    }
  }
  implicit val recipeReads = Json.reads[models.Recipe]
  implicit val recipeWrites = Json.writes[models.Recipe]

  def validateJson[A: Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  def createRecipe() = Action.async(validateJson[models.Recipe]) { request =>
    recipeService.create(request.body).map(id => Ok(Json.toJson(id).toString()))
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
