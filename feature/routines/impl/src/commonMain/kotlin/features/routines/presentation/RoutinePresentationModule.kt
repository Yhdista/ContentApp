package features.routines.presentation

import org.koin.dsl.module

fun routinePresentationModule() = module {
    factory { p -> RoutineLibraryViewModel(p.getOrNull<String>(), get(), get(), get(), get()) }
    factory { p -> RoutineEditorViewModel(routineEditorController(p.get<String>(), get(), get(), get()), get(), get(), get()) }
    single<features.routines.api.RoutineDraftFactory> { WorkoutRoutineDraftFactory(get(), get()) }
}
