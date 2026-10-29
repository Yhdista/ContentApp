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
        for source in (
            'import android.os.Bundle',
            'val leak = android.os.Build.VERSION',
            'val leak = java.awt.Color.RED',
            'import javax.crypto.Cipher',
            'import UIKit.UIView',
            'val leak = Foundation.NSString()',
            'val leak = platform.UIKit.UIView()',
            'import platform.UIKit.UIView as NativeView',
            'import platform as native\nval leak = native.UIKit.UIView()',
            'import kotlinx.cinterop.CPointer',
        ):
            with self.subTest(source=source):
                self.check_fixture(self.kotlin('feature/a/api', 'Leak.kt', 'package features.a\n' + source), 'common-platform-import')

    def test_shared_platform_namespace_segments_are_allowed(self):
        files = self.kotlin('designsystem', 'Navigation.kt', '''package ui.designsystem.organisms
import androidx.compose.ui.platform.LocalDensity
val density = androidx.compose.ui.platform.LocalDensity
''')
        files.update(self.kotlin('feature/a/impl', 'SetRowItem.kt', '''package features.a
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
'''))
        files.update(self.kotlin('feature/a/impl', 'WorkoutHistoryViewModel.kt', '''package features.a
import core.platform.PlatformClipboard
import core.platform.NoOpPlatformClipboard
val clipboard: core.platform.PlatformClipboard? = null
'''))
        self.check_fixture(files)

    def test_material_import_alias_qualified_and_package_alias(self):
        for source in ('import androidx.compose.material3.Text as Label', 'fun x() = androidx.compose.material3.Text("x")', 'import androidx.compose as ui\nfun x() = ui.material3.Text("x")'):
            self.check_fixture(self.kotlin('feature/a/impl', 'Leak.kt', 'package features.a\n' + source), 'material-ownership')

    def test_atomic_nucleus_cannot_call_atoms_through_imports_or_aliases(self):
        for source in (
            'import ui.designsystem.atoms.*\nfun use() = AppButton()',
            'import ui.designsystem.atoms.AppButton as Action\nfun use() = Action()',
            'fun use() = ui.designsystem.atoms.AppButton()',
            'import ui.designsystem as ds\nfun use() = ds.atoms.AppButton()',
        ):
            with self.subTest(source=source):
                files = self.kotlin('designsystem', 'atoms/Controls.kt', 'package ui.designsystem.atoms\nfun AppButton() = Unit')
                files.update(self.kotlin('designsystem', 'nucleus/Adapter.kt', 'package ui.designsystem.nucleus\n' + source))
                self.check_fixture(files, 'atomic-layer-direction: nucleus -> atoms')

    def test_atomic_all_upward_edges_are_rejected(self):
        layers = ('nucleus', 'atoms', 'molecules', 'organisms', 'templates')
        for index, layer in enumerate(layers):
            for target in layers[index + 1:]:
                with self.subTest(layer=layer, target=target):
                    source = f'package ui.designsystem.{layer}\nval leak = ui.designsystem.{target}.Widget()'
                    self.check_fixture(self.kotlin('designsystem', layer + '/Leak.kt', source), 'atomic-layer-direction')

    def test_atomic_ownership_follows_physical_directories(self):
        # A high declared package cannot grant a physically low adapter high-layer access.
        self.check_fixture(self.kotlin('designsystem', 'nucleus/Leak.kt', 'package ui.designsystem.templates\nimport ui.designsystem.atoms.*'), 'atomic-layer-direction')
        # A low declared package cannot hide a physically high implementation from an atom.
        files = self.kotlin('designsystem', 'organisms/Hidden.kt', 'package ui.designsystem.nucleus\nclass Hidden')
        files.update(self.kotlin('designsystem', 'atoms/Use.kt', 'package ui.designsystem.atoms\nimport ui.designsystem.nucleus.Hidden'))
        self.check_fixture(files, 'atomic-layer-direction: atoms -> organisms')

    def test_atomic_same_lower_layers_and_ignored_literals_are_allowed(self):
        files = {}
        layers = ('nucleus', 'atoms', 'molecules', 'organisms', 'templates')
        for index, layer in enumerate(layers):
            source = f'package ui.designsystem.{layer}\n'
            source += f'import ui.designsystem.{layer}.peer.*\n'
            if index:
                source += f'import ui.designsystem.{layers[index - 1]}.Component as Lower\n'
            source += 'class Component\n'
            source += '// ui.designsystem.templates.Forbidden()\n/* ui.designsystem.organisms.AlsoIgnored */\n'
            source += 'val example = "ui.designsystem.templates.Forbidden()"\n'
            source += 'val raw = """import ui.designsystem.templates.*"""\n'
            files.update(self.kotlin('designsystem', layer + '/Component.kt', source))
        self.check_fixture(files)
        # Known symbols use physical ownership even if the package name suggests a higher rank.
        files = self.kotlin('designsystem', 'nucleus/Neutral.kt', 'package ui.designsystem.templates\nclass Neutral')
        files.update(self.kotlin('designsystem', 'atoms/Use.kt', 'package ui.designsystem.atoms\nval token = ui.designsystem.templates.Neutral()'))
        self.check_fixture(files)

    def test_ui_room_dao_and_network(self):
        for imported in ('androidx.room.Room', 'features.a.data.WorkoutDao', 'features.a.data.dao.*', 'io.ktor.client.HttpClient'):
            self.check_fixture(self.kotlin('feature/a/impl', 'ThingScreen.kt', 'package features.a\nimport ' + imported), 'ui-storage-ownership')

    def test_ui_workflow_participants(self):
        for filename in ('ThingScreen.kt', 'ThingRoot.kt', 'ThingViewModel.kt'):
            self.check_fixture(self.kotlin('feature/a/impl', filename, 'package features.a\nimport features.workout.domain.WorkoutStore as Store'), 'ui-workflow-ownership')

    def test_workflow_participants_cannot_escape_to_foreign_ordinary_files(self):
        for name, package in (('WorkoutStore', 'features.workout.domain'), ('ExerciseCatalogImport', 'features.exercises.api')):
            for filename in ('ThingUseCase.kt', 'data/Thing.kt'):
                for reference in (
                    f'import {package}.{name}',
                    f'import {package}.{name} as Participant\nval participant: Participant? = null',
                    f'val participant: {package}.{name}? = null',
                    f'import {package}.*\nval participant: {name}? = null',
                    f'import {package} as workflow\nval participant: workflow.{name}? = null',
                ):
                    with self.subTest(name=name, filename=filename, reference=reference):
                        self.check_fixture(self.kotlin('feature/a/impl', filename, 'package features.a\n' + reference), 'workflow-module-ownership')

    def test_workflow_participants_are_owned_by_their_modules_and_app(self):
        for name, package, feature_name in (('WorkoutStore', 'features.workout.domain', 'workout'), ('ExerciseCatalogImport', 'features.exercises.api', 'exercises')):
            for module in (f'feature/{feature_name}/api', f'feature/{feature_name}/impl', 'composeApp'):
                with self.subTest(name=name, module=module):
                    self.check_fixture(self.kotlin(module, 'Participant.kt', f'package own\nimport {package}.{name}'))
            for filename in ('ThingScreen.kt', 'ThingRoot.kt', 'ThingViewModel.kt'):
                with self.subTest(name=name, filename=filename):
                    self.check_fixture(self.kotlin(f'feature/{feature_name}/impl', filename, f'package own\nimport {package}.*\nval participant: {name}? = null'), 'ui-workflow-ownership')

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
