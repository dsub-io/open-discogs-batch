"""Regression coverage for skipping application tests without hiding changes."""

import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

from classify_changes import classify_event, classify_paths


class PathTests(unittest.TestCase):
    def test_prose_and_ci_do_not_run_application_checks(self):
        for path in ("README.md", "AGENTS.md", "SECURITY.md", "CONTRIBUTING.md",
                     "CHANGELOG.md", "LICENSE", "NOTICE", "docs/guide.md",
                     "docs/images/example.svg"):
            with self.subTest(path=path):
                self.assertEqual(classify_paths([path]), (False, False))
        for path in (".github/workflows/ci.yml", ".github/dependabot.yml",
                     ".github/scripts/classify_changes.py",
                     "release-please-config.json", ".release-please-manifest.json"):
            with self.subTest(path=path):
                self.assertEqual(classify_paths([path]), (False, True))

    def test_runtime_build_and_contract_changes_run_application_checks(self):
        for path in ("main.go", "internal/config/config_test.go", "src/test/fixture.md",
                     "go.mod", "go.sum", "build.gradle", "gradlew.bat",
                     "version.txt", ".env.example", "Dockerfile", ".dockerignore",
                     ".goreleaser.yaml", "compose.yaml", "deploy/config.yaml",
                     "schema/migrations/V001.sql", "schema/contracts/import-manifest-v1.md",
                     "schema/contracts/vectors.json", "docs/fixtures/input.sql",
                     "scripts/verify-generated-models.sh", "unknown.file"):
            with self.subTest(path=path):
                self.assertEqual(classify_paths([path]), (True, False))

    def test_mixed_changes_and_empty_diff(self):
        self.assertEqual(classify_paths(["README.md", ".github/workflows/ci.yml"]),
                         (False, True))
        self.assertEqual(classify_paths(["README.md", "main.go", ".github/workflows/ci.yml"]),
                         (True, True))
        self.assertEqual(classify_paths([]), (False, False))


class EventTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.previous = Path.cwd()
        os.chdir(self.temp.name)
        self.addCleanup(self.temp.cleanup)
        self.addCleanup(os.chdir, self.previous)
        self.git("init", "--quiet", "-b", "main")
        self.git("config", "user.email", "ci@example.invalid")
        self.git("config", "user.name", "CI fixture")
        self.write("README.md", "Initial documentation\n")
        self.write("main.go", "package main\n")
        self.base = self.commit()

    def git(self, *args):
        return subprocess.check_output(["git", *args], stderr=subprocess.PIPE).decode().strip()

    def write(self, path, content):
        target = Path(path)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content)

    def commit(self):
        self.git("add", "-A")
        self.git("-c", "commit.gpgsign=false", "commit", "--quiet", "-m", "ci: fixture")
        return self.git("rev-parse", "HEAD")

    def pr(self, head, **fields):
        return {"pull_request": {"base": {"sha": self.base}, "head": {"sha": head}},
                **fields}

    def test_docs_and_ci_pr(self):
        self.write("README.md", "Updated documentation\n")
        self.write(".github/workflows/ci.yml", "name: CI\n")
        result = classify_event("pull_request", self.pr(self.commit()))
        self.assertEqual((result["heavy"], result["ci"]), ("false", "true"))

    def test_deleted_source_and_renamed_source_still_run_tests(self):
        Path("docs").mkdir(exist_ok=True)
        Path("main.go").rename("docs/example.md")
        self.assertEqual(classify_event("pull_request", self.pr(self.commit()))["heavy"], "true")
        Path("docs/example.md").unlink()
        self.assertEqual(classify_event("pull_request", self.pr(self.commit()))["heavy"], "true")

    def test_base_branch_advance_is_not_a_pr_change(self):
        self.git("checkout", "--quiet", "-b", "docs")
        self.write("README.md", "PR documentation\n")
        head = self.commit()
        self.git("checkout", "--quiet", "main")
        self.write("main.go", "package main\n// changed on main\n")
        self.base = self.commit()
        self.assertEqual(classify_event("pull_request", self.pr(head))["heavy"], "false")

    def test_title_and_body_edits_skip_tests_but_retargeting_does_not(self):
        self.write("main.go", "package main\n// changed in PR\n")
        head = self.commit()
        for field in ("title", "body"):
            event = self.pr(head, action="edited", changes={field: {"from": "old"}})
            self.assertEqual(classify_event("pull_request", event)["heavy"], "false")
        event = self.pr(head, action="edited", changes={"base": {"ref": {"from": "beta"}}})
        self.assertEqual(classify_event("pull_request", event)["heavy"], "true")

    def test_merge_queue_uses_its_combined_diff(self):
        self.write("README.md", "Queue documentation\n")
        head = self.commit()
        event = {"merge_group": {"base_sha": self.base, "head_sha": head}}
        self.assertEqual(classify_event("merge_group", event)["heavy"], "false")
        self.write("main.go", "package main\n// queue code\n")
        event["merge_group"]["head_sha"] = self.commit()
        self.assertEqual(classify_event("merge_group", event)["heavy"], "true")

    def test_manual_and_scheduled_runs_verify_everything(self):
        for event in ("workflow_dispatch", "schedule"):
            self.assertEqual(classify_event(event, {}),
                             {"heavy": "true", "ci": "true", "range": ""})

    def test_diff_errors_and_invalid_shas_fail(self):
        event = self.pr("0" * 40)
        with self.assertRaises(subprocess.CalledProcessError):
            classify_event("pull_request", event)
        with self.assertRaises(ValueError):
            classify_event("pull_request", self.pr("--help"))

    def test_whitespace_errors_are_not_silently_skipped(self):
        self.write("README.md", "Trailing spaces   \n")
        with self.assertRaises(subprocess.CalledProcessError):
            classify_event("pull_request", self.pr(self.commit()))

    def test_output_file_contains_routing_results(self):
        import classify_changes
        self.write("event.json", "{}")
        with patch.dict(os.environ, {"GITHUB_EVENT_NAME": "workflow_dispatch",
                                     "GITHUB_EVENT_PATH": "event.json",
                                     "GITHUB_OUTPUT": "outputs.txt"}):
            classify_changes.main()
        self.assertEqual(Path("outputs.txt").read_text(), "heavy=true\nci=true\nrange=\n")


if __name__ == "__main__":
    unittest.main()
