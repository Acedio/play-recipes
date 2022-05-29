package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._

import play.api.Application
import play.api.db.{Database, Databases}
import play.api.db.evolutions.{Evolution, Evolutions, SimpleEvolutionsReader}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test._

import controllers.RecipesController
import models.{RecipeRepository,FakeRecipeRepository}

/** Functional tests for RecipesController.
 */
class FunctionalSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting {
  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .overrides(bind[RecipeRepository].to[FakeRecipeRepository])
      .build()
  }

  val recipesController = app.injector.instanceOf[RecipesController]

  "RecipesController" should {
    "get recipe from the DB" in {
      val result = recipesController.getRecipe(1).apply(FakeRequest())
      status(result) must equal(OK)
      (contentAsJson(result) \ "message").as[String] must equal ("Recipe details by id")
    }

    "missing recipe from the DB" in {
      val result = recipesController.getRecipe(4).apply(FakeRequest())
      status(result) must equal(OK)
      (contentAsJson(result) \ "message").as[String] must equal ("No recipe found")
    }
  }
}
