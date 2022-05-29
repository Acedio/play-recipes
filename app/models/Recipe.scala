package models

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import play.api.libs.functional.syntax._
import play.api.libs.json._

// This should be kept up to date with the `recipes` table in Recipes.sql.
case class Recipe(
    id: Option[Long],
    title: String,
    making_time: String,
    serves: String,
    ingredients: String,
    cost: Long,
    created_at: Option[DateTime],
    updated_at: Option[DateTime]
)

// Functions for converting Recipes to and from JSON.
object Recipe {
  // TODO: Check again to see if there's a built in formatter for JodaTime.
  implicit val dateTimeFormat = new Format[DateTime] {
    val DateFormat: String = "yyyy-MM-dd HH:mm:ss"

    def writes(dt: DateTime): JsValue =
      JsString(DateTimeFormat.forPattern(DateFormat).print(dt))

    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsString(s) =>
        JsSuccess(DateTimeFormat.forPattern(DateFormat).parseDateTime(s))
      case js =>
        JsError(
          Seq(
            JsPath ->
              Seq(JsonValidationError("error.expected.date.isoformat", js))
          )
        )
    }
  }

  implicit val recipeFormat: Format[Recipe] = {
    val recipeReads = Json.reads[Recipe]

    // Can't use an auto-generated Writes because output `cost` needs to be a
    // String (while input and storage use integers).
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

    Format(recipeReads, recipeWrites)
  }

  // Utility transformers to adapt Recipes to fit per-method differences.
  def withoutTimestamps(r: Recipe): Recipe = Recipe(
    r.id,
    r.title,
    r.making_time,
    r.serves,
    r.ingredients,
    r.cost,
    None,
    None
  )

  def withoutId(r: Recipe): Recipe = Recipe(
    None,
    r.title,
    r.making_time,
    r.serves,
    r.ingredients,
    r.cost,
    r.created_at,
    r.updated_at
  )
}
