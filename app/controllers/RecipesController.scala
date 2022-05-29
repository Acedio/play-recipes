package controllers

import javax.inject._

import scala.concurrent.{ExecutionContext, Future}

import play.api._
import play.api.libs.json._
import play.api.mvc._

import models._

/** This controller handles requests to create, list, and modify Recipes.
  */
@Singleton
class RecipesController @Inject() (
    recipeService: RecipeRepository,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController {
  // The spec says we should return 200 SUCCESS for all endpoints, and indeed
  // the tests expect us to return 200 in the invalid request case. Use `error`
  // to signify in the code where the error cases are in case this isn't
  // actually desired.
  private def error(message: String): Result =
    Ok(Json.obj("message" -> message))

  // Could use a custom BodyParser for POST and UPDATE, but since the response
  // behavior varies we implement it per-method.
  def createRecipe() = Action.async(parse.json) { request =>
    {
      // Check that all required fields exist.
      val parsedOr: Either[Result, Recipe] =
        // Tests require a 200 SUCCESS here rather than a 400 INVALID REQUEST.
        request.body
          .asOpt[Recipe]
          .toRight(
            Ok(
              Json.obj(
                "message" -> "Recipe creation failed!",
                "required" -> "title, making_time, serves, ingredients, cost"
              )
            )
          )
      // Create the recipe.
      val idOr: Future[Either[Result, Long]] = parsedOr match {
        case Left(result) => Future.successful(Left(result))
        case Right(recipe) =>
          recipeService
            .create(recipe)
            .map(
              _.toRight(error("Could not create recipe."))
            )
      }
      // If successful, fetch the created recipe.
      val recipeOr: Future[Either[Result, Recipe]] = idOr
        .flatMap(_ match {
          case Left(result) => Future.successful(Left(result))
          case Right(id) =>
            recipeService
              .get(id)
              .map(
                _.toRight(
                  error("Recipe was unexpectedly missing")
                )
              )
        })
      // Format the response.
      recipeOr.map(
        _.fold(
          identity,
          r =>
            Ok(
              Json.obj(
                "message" -> "Recipe successfully created!",
                "recipe" -> List(r)
              )
            )
        )
      )
    }
  }

  def listRecipes() = Action.async {
    recipeService
      .list()
      .map(rs =>
        Ok(
          Json.obj(
            "recipes" -> rs.map(Recipe.withoutTimestamps)
          )
        )
      )
  }

  def getRecipe(id: Long) = Action.async {
    recipeService
      .get(id)
      .map(
        _.map(r =>
          Ok(
            Json.obj(
              "message" -> "Recipe details by id",
              "recipe" -> List(Recipe.withoutTimestamps(r))
            )
          )
        ).getOrElse(error("No recipe found"))
      )
  }

  def updateRecipe(id: Long) = Action.async(parse.json) { request =>
    {
      // Check that all required fields exist.
      val parsedOr: Either[Result, Recipe] =
        request.body
          .asOpt[Recipe]
          .toRight(
            Ok(
              Json.obj(
                "message" -> "Recipe update failed!",
                "required" -> "title, making_time, serves, ingredients, cost"
              )
            )
          )
      val idOr: Future[Either[Result, Long]] = parsedOr match {
        case Left(result) => Future.successful(Left(result))
        case Right(recipe) =>
          recipeService
            .update(id, recipe)
            .map(Either.cond(_, id, error("No recipe found")))
      }
      // If successful, fetch the updated recipe.
      val recipeOr: Future[Either[Result, Recipe]] = idOr
        .flatMap(_ match {
          case Left(result) => Future.successful(Left(result))
          case Right(id) =>
            recipeService
              .get(id)
              .map(
                _.toRight(
                  error("Recipe was unexpectedly missing")
                )
              )
        })
      // Format the response.
      recipeOr.map(
        _.fold(
          identity,
          r =>
            Ok(
              Json.obj(
                "message" -> "Recipe successfully updated!",
                "recipe" -> List(Recipe.withoutId(Recipe.withoutTimestamps(r)))
              )
            )
        )
      )
    }
  }

  def deleteRecipe(id: Long) = Action.async {
    recipeService
      .delete(id)
      .map(
        if (_) Ok(Json.obj("message" -> "Recipe successfully removed!"))
        else error("No recipe found")
      )
  }
}
