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
  cost: String,
  created_at: Option[DateTime],
  updated_at: Option[DateTime]
)

trait RecipeRepository {
  def create(recipe: Recipe) : Future[Long]
  def update(id: Long, recipe: Recipe) : Future[Boolean]
  def list(): Future[Iterable[Recipe]]
  def get(id: Long) : Future[Option[Recipe]]
  def delete(id: Long) : Future[Boolean]
}

@Singleton
class DatabaseRecipeRepository @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext) extends RecipeRepository {
  private val db = dbapi.database("default")

  override def create(recipe: Recipe): Future[Long] = {
    Future {
      db.withConnection {
        implicit connection =>
          SQL(
            """
            INSERT INTO recipes(title, making_time, serves, ingredients, cost)
            VALUES ({title}, {making_time}, {serves}, {ingredients}, {cost})
            """
          )
            .on("title" -> recipe.title,
                "making_time" -> recipe.making_time,
                "serves" -> recipe.serves,
                "ingredients" -> recipe.ingredients,
                "cost" -> recipe.cost)
            .executeInsert().getOrElse(-1)
      }
    }
  }
  override def update(id: Long, recipe: Recipe) : Future[Boolean] = {
    Future {
      db.withConnection {
        implicit connection =>
          SQL(
            """
            UPDATE recipes
            SET title = {title},
                making_time = {making_time},
                serves = {serves},
                ingredients = {ingredients},
                cost = {cost}
            WHERE id = {id}
            """
          )
            .on("id" -> id,
                "title" -> recipe.title,
                "making_time" -> recipe.making_time,
                "serves" -> recipe.serves,
                "ingredients" -> recipe.ingredients,
                "cost" -> recipe.cost)
            .executeUpdate() == 1
      }
    }
  }
  override def list(): Future[Iterable[Recipe]] = {
    Future {
      db.withConnection {
        implicit connection => {
          val parser: RowParser[Recipe] = Macro.namedParser[Recipe]
          SQL"SELECT id, title, making_time, serves, ingredients, cost, created_at, updated_at FROM recipes".as(parser.*)
        }
      }
    }
  }
  override def get(id: Long): Future[Option[Recipe]] = {
    Future {
      db.withConnection {
        implicit connection => {
          val parser: RowParser[Recipe] = Macro.namedParser[Recipe]
          val result: List[Recipe] = SQL"SELECT id, title, making_time, serves, ingredients, cost, created_at, updated_at FROM recipes WHERE id = $id".as(parser.*)
          result.headOption
        }
      }
    }
  }
  override def delete(id: Long): Future[Boolean] = {
    Future {
      db.withConnection {
        implicit connection => {
          SQL("DELETE FROM recipes WHERE id = {id}").on("id" -> id).executeUpdate() == 1
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
      "1000",
      None,
      None
    ),
    Recipe(
      Some(2),
      "Rice Omelette",
      "30 min",
      "2 people",
      "onion, egg, seasoning, soy sauce",
      "700",
      None,
      None
    )
  )

  override def create(recipe: Recipe): Future[Long] = {
    Future { 1 }
  }
  override def update(id: Long, recipe: Recipe) : Future[Boolean] = {
    Future {
      false
    }
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
  override def delete(id: Long): Future[Boolean] = {
    Future {
      false
    }
  }
}
