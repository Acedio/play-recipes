package controllers

import javax.inject._

import play.api._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.concurrent.{ExecutionContext, Future}

import models._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
// TODO: val for recipeService? No val for controllerComponents?
class RecipesController @Inject()(recipeService: RecipeRepository, 
                                  val controllerComponents: ControllerComponents)(implicit ec: ExecutionContext)
    extends BaseController {
  val logger = Logger(this.getClass())

  // TODO: Check again to see if there's a built in formatter for JodaTime.
  implicit val dateTimeFormat = new Format[DateTime] {
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

  val recipeReads = Json.reads[Recipe]

  // Can't use an auto-generated Writes because output cost needs to be a String
  // (while input and storage use integers).
  val recipeWrites: Writes[Recipe] = (
    (JsPath \ "id").writeNullable[Long] and
    (JsPath \ "title").write[String] and
    (JsPath \ "making_time").write[String] and
    (JsPath \ "serves").write[String] and
    (JsPath \ "ingredients").write[String] and
    (JsPath \ "cost").write[String].contramap[Long](_.toString) and
    (JsPath \ "created_at").writeNullable[DateTime] and
    (JsPath \ "updated_at").writeNullable[DateTime]
  )(unlift(Recipe.unapply))

  implicit val recipeFormat: Format[Recipe] = Format(recipeReads, recipeWrites)

  // Spec requires returning 200 SUCCESS (with an error message) on a parse
  // error.
  def validateRecipe = parse.json.validate(
    _.validate[Recipe].asEither.left.map(e => Ok(
      Json.obj(
        "message" -> "Recipe creation failed!",
        "required" -> "title, making_time, serves, ingredients, cost"
      )
    ))
  )

  def withoutTimestamps(r: Recipe): Recipe = Recipe(
    r.id,
    r.title,
    r.making_time,
    r.serves,
    r.ingredients,
    r.cost,
    None,
    None)

  def withoutId(r: Recipe): Recipe = Recipe(
    None,
    r.title,
    r.making_time,
    r.serves,
    r.ingredients,
    r.cost,
    r.created_at,
    r.updated_at)

  // This doesn't use validateJson because caller expects a 200 response even on
  // invalid request.
  def createRecipe() = Action.async(parse.json) {
    request => {
      logger.warn(request.body.toString())
      val parsed: Option[Recipe] = request.body.asOpt(recipeReads)
      val parsedOr: Either[Result, Recipe] = parsed.toRight(Ok(
        Json.obj(
          "message" -> "Recipe creation failed!",
          "required" -> "title, making_time, serves, ingredients, cost"
        )
      ))
      val resp = parsedOr match {
        case Left(resp) => Future.successful(resp)
        case Right(recipe) => {
          val id: Future[Long] = recipeService.create(recipe)
          val createdRecipe: Future[Option[Recipe]] = id.flatMap(id => recipeService.get(id))
          createdRecipe.map(
            _.map(
              r => Ok(Json.obj(
                "message" -> "Recipe successfully created!",
                "recipe" -> List(r)
              ))
            ).getOrElse(InternalServerError)
          )
        }
      }
      resp.foreach(r => logger.warn(r.body.toString()))
      resp
    }
  }

  def listRecipes() = Action.async {
    recipeService.list().map(rs => Ok(Json.obj(
      "recipes" -> rs.map(withoutTimestamps)
    )))
  }

  def getRecipe(id: Long) = Action.async {
    recipeService.get(id).map(
      _.map(
        r => Ok(Json.obj(
          "message" -> "Recipe details by id",
          "recipe" -> List(withoutTimestamps(r))
        ))
      ).getOrElse(NotFound)
    )
  }

  def updateRecipe(id: Long) = Action.async(validateRecipe) {
    request => {
      val wasUpdated: Future[Boolean] = recipeService.update(id, request.body)
      // If the update succeeded, go and grab the updated Recipe.
      val recipeOr: Future[Either[Result, Recipe]] = wasUpdated
        .map(Either.cond(_, id, NotFound))
        .flatMap({
          case Left(resp) => Future.successful(Left(resp))
          case Right(id) => recipeService.get(id).map(_.toRight(InternalServerError))
        })
      // Format the response.
      recipeOr.map(
        _.map(
          (r: Recipe) => Ok(Json.obj(
            "message" -> "Recipe successfully updated!",
            "recipe" -> List(withoutId(withoutTimestamps(r)))
          ))
        )
        .fold(identity, identity)
      )
    }
  }

  def deleteRecipe(id: Long) = Action.async {
    recipeService.delete(id).map(
      _ match {
        case true => Ok(Json.obj("message" -> "Recipe successfully removed!"))
        case false => NotFound(Json.obj("message" -> "No recipe found"))
      }
    )
  }
}
