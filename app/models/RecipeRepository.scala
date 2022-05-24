package models

import javax.inject._

import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

case class Recipe(
  id: Option[Long],
  title: String,
  making_time: String,
  serves: String,
  ingredients: String,
  cost: Long,
  // TODO: timestamps should be DateTimes.
  created_at: Option[String],
  updated_at: Option[String]
)

trait RecipeRepository {
  def create(recipe: Recipe) : Future[Long]
  def list(): Future[Iterable[Recipe]]
  def get(id: Long) : Future[Option[Recipe]]
  // TODO: delete(id: Long)
}

@Singleton
class InMemoryRecipeRepository @Inject()()(implicit ec: ExecutionContext) extends RecipeRepository {
  private val logger = Logger(this.getClass)

  private val recipeList = List(
    Recipe(
      Some(1),
      "Chicken Curry",
      "45 min",
      "4 people",
      "onion, chicken, seasoning",
      1000,
      Some("2016-01-10 12:10:12"),
      Some("2016-01-10 12:10:12")
    ),
    Recipe(
      Some(2),
      "Rice Omelette",
      "30 min",
      "2 people",
      "onion, egg, seasoning, soy sauce",
      700,
      Some("2016-01-11 13:10:12"),
      Some("2016-01-11 13:10:12")
    )
  )

  override def create(recipe: Recipe): Future[Long] = {
    Future { 1 }
  }
  override def list(): Future[Iterable[Recipe]] = {
    Future {
      recipeList
    }
  }
  override def get(id: Long): Future[Option[Recipe]] = {
    Future {
      logger.info("here!")
      recipeList.find(recipe => recipe.id == Some(id))
    }
  }
}
