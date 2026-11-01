"""Black-box contract tests for the Phase 3 changed-UI policy checker."""

from __future__ import annotations

import re
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("phase3_ui_check.py")


class Phase3UiCheckTests(unittest.TestCase):
    def setUp(self) -> None:
        self.repo = Path(tempfile.mkdtemp(prefix="phase3-ui-check-"))
        self.run_git("init", "-q")
        self.run_git("config", "user.email", "checker@example.invalid")
        self.run_git("config", "user.name", "Phase 3 checker test")
        self.write("README.md", "baseline\n")
        self.run_git("add", ".")
        self.run_git("commit", "-qm", "base")
        self.run_git("branch", "phase3/integration")

    def tearDown(self) -> None:
        shutil.rmtree(self.repo)

    def run_git(self, *args: str) -> None:
        subprocess.run(
            ["git", *args], cwd=self.repo, check=True, capture_output=True, text=True
        )

    def write(self, relative_path: str, contents: str) -> None:
        path = self.repo / relative_path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(contents, encoding="utf-8")

    def write_bytes(self, relative_path: str, contents: bytes) -> None:
        path = self.repo / relative_path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(contents)

    def commit_change(self, relative_path: str, contents: str) -> None:
        self.write(relative_path, contents)
        self.run_git("add", ".")
        self.run_git("commit", "-qm", "changed file")

    def commit_base_change(self, relative_path: str, contents: str) -> None:
        self.commit_change(relative_path, contents)
        self.run_git("branch", "-f", "phase3/integration")

    def check(self) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(SCRIPT), "--base-ref", "phase3/integration"],
            cwd=self.repo,
            capture_output=True,
            text=True,
        )

    def assert_clean(self) -> None:
        result = self.check()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def assert_violation(self, rule: str, path: str) -> None:
        result = self.check()
        self.assertNotEqual(result.returncode, 0)
        output = result.stdout + result.stderr
        self.assertIn(rule, output)
        self.assertIn(path, output)
        self.assertRegex(output, rf"{re.escape(path)}:\d+:")

    def test_allows_clean_changed_file(self) -> None:
        self.commit_change("feature/EntryScreen.kt", "fun render() = Unit\n")

        self.assert_clean()

    def test_rejects_raw_colour_outside_theme_tokens(self) -> None:
        self.commit_change(
            "feature/EntryScreen.kt", "val accent = Color(0x" + "FF123456)\n"
        )

        self.assert_violation("raw-colour", "feature/EntryScreen.kt")

    def test_rejects_named_raw_colour_outside_theme_tokens(self) -> None:
        self.commit_change("feature/EntryScreen.kt", "val accent = Color.Red\n")

        self.assert_violation("raw-colour", "feature/EntryScreen.kt")

    def test_rejects_numeric_channel_raw_colour_outside_theme_tokens(self) -> None:
        self.commit_change("feature/EntryScreen.kt", "val accent = Color(255, 0, 0)\n")

        self.assert_violation("raw-colour", "feature/EntryScreen.kt")

    def test_rejects_underscore_hex_raw_colour_outside_theme_tokens(self) -> None:
        self.commit_change(
            "feature/EntryScreen.kt", "val accent = Color(0xFF_12_34_56)\n"
        )

        self.assert_violation("raw-colour", "feature/EntryScreen.kt")

    def test_allows_raw_colour_definition_in_theme_tokens(self) -> None:
        self.commit_change(
            "ui/theme/Color.kt", "val HarvestGreen = Color(0x" + "FF123456)\n"
        )

        self.assert_clean()

    def test_rejects_emoji_in_changed_content(self) -> None:
        self.commit_change("feature/EntryScreen.kt", 'Text("Ready ' + chr(0x1F642) + '")\n')

        self.assert_violation("emoji", "feature/EntryScreen.kt")

    def test_rejects_keycap_emoji_sequence(self) -> None:
        keycap = "1" + "\ufe0f" + "\u20e3"
        self.commit_change("feature/EntryScreen.kt", f'Text("{keycap}")\n')

        self.assert_violation("emoji", "feature/EntryScreen.kt")

    def test_rejects_zwj_emoji_sequence(self) -> None:
        family = "\U0001f468" + "\u200d" + "\U0001f469" + "\u200d" + "\U0001f467"
        self.commit_change("feature/EntryScreen.kt", f'Text("{family}")\n')

        self.assert_violation("emoji", "feature/EntryScreen.kt")

    def test_allows_ordinary_dingbat_symbol(self) -> None:
        self.commit_change(
            "feature/EntryScreen.kt", 'Text("Checked ' + chr(0x2713) + '")\n'
        )

        self.assert_clean()

    def test_rejects_paisa_arithmetic_in_a_screen(self) -> None:
        self.commit_change(
            "feature/EntryScreen.kt", "val totalPaisa = salePaisa + feePaisa\n"
        )

        self.assert_violation("paisa-gram-arithmetic", "feature/EntryScreen.kt")

    def test_rejects_gram_arithmetic_in_a_sheet(self) -> None:
        self.commit_change(
            "feature/WeightSheet.kt", "val netGrams = grossGrams - tareGrams\n"
        )

        self.assert_violation("paisa-gram-arithmetic", "feature/WeightSheet.kt")

    def test_rejects_paisa_arithmetic_in_a_dialog(self) -> None:
        self.commit_change(
            "feature/ConfirmDialog.kt", "val duePaisa = totalPaisa / 2\n"
        )

        self.assert_violation("paisa-gram-arithmetic", "feature/ConfirmDialog.kt")

    def test_rejects_multiline_paisa_arithmetic_in_a_screen(self) -> None:
        self.commit_change(
            "feature/EntryScreen.kt",
            "val total =\n    salePaisa\n        +\n    feePaisa\n",
        )

        self.assert_violation("paisa-gram-arithmetic", "feature/EntryScreen.kt")

    def test_allows_lambda_arrow_with_paisa_name(self) -> None:
        self.commit_change(
            "feature/EntryScreen.kt", "entries.map { paisa -> paisa.toString() }\n"
        )

        self.assert_clean()

    def test_allows_paisa_arithmetic_text_in_comments_and_strings(self) -> None:
        self.commit_change(
            "feature/EntryScreen.kt",
            "// totalPaisa = salePaisa + feePaisa\n"
            "val help = \"totalPaisa = salePaisa + feePaisa\"\n"
            "/* totalPaisa = salePaisa + feePaisa */\n",
        )

        self.assert_clean()

    def test_allows_paisa_arithmetic_outside_ui_target_files(self) -> None:
        self.commit_change(
            "feature/EntryCalculator.kt", "val totalPaisa = salePaisa + feePaisa\n"
        )

        self.assert_clean()

    def test_rejects_install_debug_command_text(self) -> None:
        self.commit_change("tools/device.sh", "./gradlew install" + "Debug\n")

        self.assert_violation("install-debug", "tools/device.sh")

    def test_ignores_preexisting_violation_not_in_changed_hunk(self) -> None:
        legacy_colour = "val oldColour = Color(0x" + "FF123456)\n"
        self.write("feature/LegacyScreen.kt", legacy_colour)
        self.run_git("add", ".")
        self.run_git("commit", "-qm", "legacy colour")
        self.run_git("branch", "-f", "phase3/integration")
        self.commit_change("feature/LegacyScreen.kt", legacy_colour + "fun render() = Unit\n")

        self.assert_clean()

    def test_uses_new_path_for_renamed_file_with_quoted_name(self) -> None:
        old_path = 'feature/Old "Quote" Screen.kt'
        new_path = 'feature/New "Quote" Screen.kt'
        self.commit_base_change(old_path, "fun render() = Unit\n" * 6)
        self.run_git("mv", old_path, new_path)
        self.write(new_path, "fun render() = Unit\n" * 5 + "val accent = Color.Red\n")
        self.run_git("add", ".")
        self.run_git("commit", "-qm", "rename and change")

        self.assert_violation("raw-colour", new_path)

    def test_ignores_deleted_file_content(self) -> None:
        self.commit_base_change(
            "feature/DeletedScreen.kt", "val accent = Color(0xFF123456)\n"
        )
        self.run_git("rm", "feature/DeletedScreen.kt")
        self.run_git("commit", "-qm", "delete old file")

        self.assert_clean()

    def test_ignores_binary_changed_content(self) -> None:
        self.write_bytes("feature/Picture.bin", b"\x00baseline\x00")
        self.run_git("add", ".")
        self.run_git("commit", "-qm", "binary base")
        self.run_git("branch", "-f", "phase3/integration")
        self.write_bytes("feature/Picture.bin", b"\x00changed\x00")
        self.run_git("add", ".")
        self.run_git("commit", "-qm", "binary change")

        self.assert_clean()


if __name__ == "__main__":
    unittest.main()
