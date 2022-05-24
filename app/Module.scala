import com.google.inject.AbstractModule
import com.google.inject.name.Names

class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[models.RecipeRepository])
      .to(classOf[models.InMemoryRecipeRepository])
  }
}
