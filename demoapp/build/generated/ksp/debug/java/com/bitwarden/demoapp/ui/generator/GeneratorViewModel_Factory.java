package com.bitwarden.demoapp.ui.generator;

import androidx.lifecycle.SavedStateHandle;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class GeneratorViewModel_Factory implements Factory<GeneratorViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private GeneratorViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public GeneratorViewModel get() {
    return newInstance(savedStateHandleProvider.get());
  }

  public static GeneratorViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new GeneratorViewModel_Factory(savedStateHandleProvider);
  }

  public static GeneratorViewModel newInstance(SavedStateHandle savedStateHandle) {
    return new GeneratorViewModel(savedStateHandle);
  }
}
