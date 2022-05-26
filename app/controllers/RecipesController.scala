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
    val DateFormat: String = "yyyy-MM-dd HH:mm:ss"

    def writes(dt: DateTime): JsValue =
      JsString(DateTimeFormat.forPattern(DateFormat).print(dt))

    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsString(s) => 
        JsSuccess(DateTimeFormat.forPattern(DateFormat).parseDateTime(s))
      case js => JsError(
        Seq(
          JsPath ->
            Seq(JsonValidationError("error.expected.date.isoformat", js))
        )
      )
    }
  }

  // TODO: Add a converter that drops datetimes.
  implicit val recipeReads = Json.reads[models.Recipe]
  implicit val recipeWrites = Json.writes[models.Recipe]

  def validateJson[A: Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  def withoutTimestamps(r: models.Recipe): models.Recipe = models.Recipe(
    r.id,
    r.title,
    r.making_time,
    r.serves,
    r.ingredients,
    r.cost,
    None,
    None)

  def withoutId(r: models.Recipe): models.Recipe = models.Recipe(
    None,
    r.title,
    r.making_time,
    r.serves,
    r.ingredients,
    r.cost,
    r.created_at,
    r.updated_at)

  // TODO: Remove the validateJson because caller expects a 200 response even on
  // invalid request.
  def createRecipe() = Action.async(validateJson[models.Recipe]) {
    request => {
      val id: Future[Long] = recipeService.create(request.body)
      val recipe: Future[Option[models.Recipe]] = id.flatMap(id => recipeService.get(id))
      recipe.map(
        _.map(
          r => Ok(Json.obj(
            "message" -> "Recipe successfully created!",
            "recipe" -> r
          ).toString())
        ).getOrElse(InternalServerError)
      )
    }
  }

  def listRecipes() = Action.async {
    recipeService.list().map(rs => Ok(Json.obj(
      "recipes" -> rs.map(withoutTimestamps)
    ).toString()))
  }

  def getRecipe(id: Long) = Action.async {
    recipeService.get(id).map(
      _.map(
        r => Ok(Json.obj(
          "message" -> "Recipe details by id",
          "recipe" -> withoutTimestamps(r)
        ).toString())
      ).getOrElse(NotFound)
    )
  }

  def updateRecipe(id: Long) = Action.async(validateJson[models.Recipe]) { request =>
    recipeService.update(id, request.body)
      .map(Either.cond(_, id, NotFound))
      .flatMap({
        case Left(resp) => Future.successful(Left(resp))
        case Right(id) => recipeService.get(id).map(_.toRight(InternalServerError))
      })
      .map(
        _.map(
          (r: models.Recipe) => Ok(Json.obj(
            "message" -> "Recipe successfully updated!",
            "recipe" -> withoutId(withoutTimestamps(r))
          ).toString())
        )
        .fold(identity, identity)
      )
  }

  def deleteRecipe(id: Long) = Action.async {
    recipeService.delete(id).map(_ match {
        case true => Ok(Json.obj("message" -> "Recipe successfully removed!"))
        case false => NotFound(Json.obj("message" -> "No recipe found"))
      }
    )
  }
}
