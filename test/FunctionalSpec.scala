package controllers

import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice._

import play.api.Application
import play.api.db.{Database, Databases}
import play.api.db.evolutions.{Evolution, Evolutions, SimpleEvolutionsReader}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Future

import controllers.RecipesController
import models.{Recipe,RecipeRepository,FakeRecipeRepository}

/** Functional tests for RecipesController.
 */
class FunctionalSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with Injecting {
  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .overrides(bind[RecipeRepository].to[FakeRecipeRepository])
      .build()
  }

  val recipesController = app.injector.instanceOf[RecipesController]
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  "RecipesController" should {
    "return a recipe" in {
      val result = recipesController.getRecipe(1).apply(FakeRequest())
      status(result) must equal(OK)
      (contentAsJson(result) \ "message").as[String] must equal ("Recipe details by id")
    }

    "not return a missing recipe" in {
      val result = recipesController.getRecipe(4).apply(FakeRequest())
      status(result) must equal(OK)
      (contentAsJson(result) \ "message").as[String] must equal ("No recipe found")
    }

    "return a list of recipes" in {
      val result = recipesController.listRecipes().apply(FakeRequest())
      status(result) must equal(OK)
      (contentAsJson(result) \ "recipes").as[Seq[JsObject]] must have length 2
    }

    "successfully create a recpie" in {
      // Can't use Writes because input cost's type is different than output's.
      val recipeJson = Json.obj(
        "title" -> "Title",
        "making_time" -> "Making time",
        "serves" -> "Serves",
        "ingredients" -> "Ingredients",
        "cost" -> 1224
      )
      val recipe = Recipe(
        Some(1),
        "Title",
        "Making time",
        "Serves",
        "Ingredients",
        1224,
        None,
        None
      )

      val mockRepository = mock[RecipeRepository]
      when(mockRepository.create(any())).thenReturn(Future.successful(Some(1)))
      when(mockRepository.get(1)).thenReturn(Future.successful(Some(recipe)))

      val recipesController = 
        new RecipesController(mockRepository, Helpers.stubControllerComponents())
      val request = FakeRequest()
        .withHeaders("Content-Type" -> "application/json")
        .withBody(recipeJson)
      val result = recipesController.createRecipe().apply(request)
      status(result) must equal(OK)
      (contentAsJson(result) \ "recipe").as[Seq[JsValue]] must equal (List(Json.toJson(recipe)))
    }
  }
}
