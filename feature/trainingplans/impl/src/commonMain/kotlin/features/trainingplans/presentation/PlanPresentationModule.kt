package features.trainingplans.presentation

import org.koin.dsl.module

fun planPresentationModule() = module {
    factory { p -> PlanLibraryViewModel(p.getOrNull<String>(), get(), get(), get(), get()) }
    factory { p -> PlanEditorViewModel(planEditorController(p.get<String>(), get(), get(), get()), get(), get()) }

}
