package models

import javax.inject._
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger
import play.api.db.DBApi

import anorm._

case class Recipe(
  id: Option[Long],
  title: String,
  making_time: String,
  serves: String,
  ingredients: String,
  cost: Long,
  // TODO: timestamps should be DateTimes.
  created_at: Option[DateTime],
  updated_at: Option[DateTime]
)

trait RecipeRepository {
  def create(recipe: Recipe) : Future[Long]
  def list(): Future[Iterable[Recipe]]
  def get(id: Long) : Future[Option[Recipe]]
  // TODO: delete(id: Long)
}

@Singleton
class DatabaseRecipeRepository @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext) extends RecipeRepository {
  private val db = dbapi.database("default")

  override def create(recipe: Recipe): Future[Long] = {
    Future {
      if (db.withConnection { implicit connection =>
        SQL("Select 1").execute()
      }) {
        1
      } else {
        0
      }
    }
  }
  override def list(): Future[Iterable[Recipe]] = {
    Future {
      Seq()
    }
  }
  override def get(id: Long): Future[Option[Recipe]] = {
    Future {
      db.withConnection {
        implicit connection => {
          val parser: RowParser[Recipe] = Macro.namedParser[Recipe]
          val result: List[Recipe] = SQL"SELECT * FROM recipes WHERE id = $id".as(parser.*)
          result.headOption
        }
      }
    }
  }
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
      None,
      None
    ),
    Recipe(
      Some(2),
      "Rice Omelette",
      "30 min",
      "2 people",
      "onion, egg, seasoning, soy sauce",
      700,
      None,
      None
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
