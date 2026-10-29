"""Executable positive/negative policy fixtures outside product source sets."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCANNER = Path(__file__).with_name('verify.py')


class ArchitectureFixtures(unittest.TestCase):
    def check_fixture(self, files, expected=None, inventory=None):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for name, content in files.items():
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content)
            command = [sys.executable, str(SCANNER), '--root', str(root)]
            if inventory:
                (root / 'inventory.json').write_text(json.dumps(inventory))
                command += ['--inventory', str(root / 'inventory.json')]
            result = subprocess.run(command, capture_output=True, text=True)
            self.assertEqual(1 if expected else 0, result.returncode, result.stdout + result.stderr)
            if expected:
                self.assertIn(expected, result.stderr)

    def kotlin(self, module, filename, source):
        return {module + '/build.gradle.kts': '', module + '/src/commonMain/kotlin/' + filename: source}

    def test_foreign_feature_multiline_dependency(self):
        self.check_fixture({'feature/a/impl/build.gradle.kts': 'implementation(\n project(\n ":feature:b:impl"\n )\n)'}, 'foreign-feature-implementation')

    def test_physical_impl_ownership_even_with_api_package(self):
        files = self.kotlin('feature/b/impl', 'Hidden.kt', 'package features.b.api\nclass Hidden')
        files.update(self.kotlin('feature/a/impl', 'Use.kt', 'package features.a\nimport features.b.api.Hidden'))
        self.check_fixture(files, 'foreign-feature-implementation-import')

    def test_actual_dependency_inventory_checks_catalog_alias(self):
        self.check_fixture({'foundation/build.gradle.kts': ''}, 'foundation-purity', [{'module': ':foundation', 'configuration': 'commonMainImplementation', 'target': 'androidx.room:room-runtime'}])

    def test_foundation_framework_and_fitness(self):
        for imported in ('androidx.compose.runtime.Composable', 'features.workout.domain.Workout'):
            with self.subTest(imported=imported):
                self.check_fixture(self.kotlin('foundation', 'Leak.kt', 'package core\nimport ' + imported), 'foundation-purity')

    def test_foundation_cannot_import_storage_through_neutral_package(self):
        files = self.kotlin('core/storage', 'MutationDao.kt', 'package core.database\ninterface MutationDao')
        files.update(self.kotlin('foundation', 'Leak.kt', 'package core\nimport core.database.MutationDao'))
        self.check_fixture(files, 'foundation-purity')

    def test_designsystem_domain_and_storage(self):
        for imported in ('features.exercises.api.Exercise', 'core.result.DataError', 'core.storage.DraftStore'):
            with self.subTest(imported=imported):
                self.check_fixture(self.kotlin('designsystem', 'Leak.kt', 'package ui\nimport ' + imported), 'designsystem-purity')

    def test_platform_import_and_fully_qualified_reference(self):
        for source in ('import android.os.Bundle', 'val leak = java.awt.Color.RED'):
            self.check_fixture(self.kotlin('feature/a/api', 'Leak.kt', 'package features.a\n' + source), 'common-platform-import')

    def test_material_import_alias_qualified_and_package_alias(self):
        for source in ('import androidx.compose.material3.Text as Label', 'fun x() = androidx.compose.material3.Text("x")', 'import androidx.compose as ui\nfun x() = ui.material3.Text("x")'):
            self.check_fixture(self.kotlin('feature/a/impl', 'Leak.kt', 'package features.a\n' + source), 'material-ownership')

    def test_ui_room_dao_and_network(self):
        for imported in ('androidx.room.Room', 'features.a.data.WorkoutDao', 'features.a.data.dao.*', 'io.ktor.client.HttpClient'):
            self.check_fixture(self.kotlin('feature/a/impl', 'ThingScreen.kt', 'package features.a\nimport ' + imported), 'ui-storage-ownership')

    def test_ui_workflow_participants(self):
        for filename in ('ThingScreen.kt', 'ThingRoot.kt', 'ThingViewModel.kt'):
            self.check_fixture(self.kotlin('feature/a/impl', filename, 'package features.a\nimport features.workout.domain.WorkoutStore as Store'), 'ui-workflow-ownership')

    def test_viewmodel_publish_protocol(self):
        for imported in ('core.storage.DraftStore', 'core.storage.CommandCoordinator'):
            self.check_fixture(self.kotlin('feature/a/impl', 'ThingViewModel.kt', 'package features.a\nimport ' + imported), 'viewmodel-publish-ownership')

    def test_legitimate_boundaries_and_ignored_literals(self):
        files = self.kotlin('feature/b/api', 'Public.kt', 'package features.b.api\nclass Public')
        files.update(self.kotlin('feature/a/impl', 'ThingScreen.kt', '''package features.a
import features.b.api.Public
// import androidx.room.Room
/* nested /* androidx.compose.material3.Text */ ignored */
val explanation = "androidx.compose.material3.Text core.storage.DraftStore java.awt.Color"
'''))
        files.update(self.kotlin('designsystem', 'nucleus/Adapters.kt', 'package ui.designsystem.nucleus\nimport androidx.compose.material3.Text'))
        files.update(self.kotlin('composeApp', 'Composition.kt', 'package app\nimport features.b.api.Public\nimport core.storage.CommandCoordinator'))
        files.update(self.kotlin('feature/a/impl', 'ControllerFactory.kt', 'package features.a\nimport core.storage.DraftStore\nimport core.storage.CommandCoordinator'))
        files['composeApp/src/desktopTest/kotlin/Test.kt'] = 'import java.awt.Color\nimport features.b.data.Hidden'
        files['feature/a/impl/build.gradle.kts'] = '''val example = """implementation(project(":feature:b:impl"))"""
// implementation(project(":feature:b:impl"))
'''
        self.check_fixture(files)


if __name__ == '__main__':
    unittest.main()
