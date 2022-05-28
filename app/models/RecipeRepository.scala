package models

import javax.inject._
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

import anorm._
import play.api.db.DBApi

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

/**
 * Represents an asyncronously accessible store of Recipes.
 */
trait RecipeRepository {
  def create(recipe: Recipe) : Future[Long]
  // TODO: Potentially Future[Unit] since Future already represents failure.
  def update(id: Long, recipe: Recipe) : Future[Boolean]
  def list(): Future[Iterable[Recipe]]
  def get(id: Long) : Future[Option[Recipe]]
  def delete(id: Long) : Future[Boolean]
}

/**
 * A RecipeRepository that uses a backing database to store Recipes.
 */
@Singleton
class DatabaseRecipeRepository @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext)
    extends RecipeRepository {
  private val db = dbapi.database("default")

  private val recipeParser: RowParser[Recipe] = Macro.namedParser[Recipe]

  override def create(recipe: Recipe): Future[Long] = {
    Future {
      db.withConnection {
        implicit connection =>
          // TODO: This should be able to return failure.
          SQL"""
            INSERT INTO recipes(title, making_time, serves, ingredients, cost)
            VALUES (${recipe.title},
                    ${recipe.making_time},
                    ${recipe.serves},
                    ${recipe.ingredients},
                    ${recipe.cost})
          """.executeInsert().getOrElse(-1)
      }
    }
  }

  override def update(id: Long, recipe: Recipe) : Future[Boolean] = {
    Future {
      db.withConnection {
        implicit connection =>
          SQL"""
            UPDATE recipes
            SET title       = ${recipe.title},
                making_time = ${recipe.making_time},
                serves      = ${recipe.serves},
                ingredients = ${recipe.ingredients},
                cost        = ${recipe.cost},
                updated_at  = CURRENT_TIMESTAMP
            WHERE id = $id
          """.executeUpdate() == 1
      }
    }
  }

  override def list(): Future[Iterable[Recipe]] = {
    Future {
      db.withConnection {
        implicit connection => {
          SQL"""
            SELECT id, title, making_time, serves, ingredients, cost, created_at, updated_at
            FROM recipes
          """.as(recipeParser.*)
        }
      }
    }
  }

  override def get(id: Long): Future[Option[Recipe]] = {
    Future {
      db.withConnection {
        implicit connection => {
          val result: List[Recipe] = SQL"""
            SELECT id, title, making_time, serves, ingredients, cost, created_at, updated_at
            FROM recipes
            WHERE id = $id
          """.as(recipeParser.*)
          result.headOption
        }
      }
    }
  }

  override def delete(id: Long): Future[Boolean] = {
    Future {
      db.withConnection {
        implicit connection => {
          SQL"DELETE FROM recipes WHERE id = $id"
            .executeUpdate() >= 1
        }
      }
    }
  }
}

/**
 * A fake implementation of RecipeRepository for tests.
 */
@Singleton
class FakeRecipeRepository @Inject()()(implicit ec: ExecutionContext)
    extends RecipeRepository {
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
      recipeList.find(recipe => recipe.id == Some(id))
    }
  }

  override def delete(id: Long): Future[Boolean] = {
    Future {
      false
    }
  }
}
