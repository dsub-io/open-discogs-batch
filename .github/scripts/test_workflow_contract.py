"""Check that workflows preserve the documented routing contract."""

from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
WORKFLOWS = ROOT / ".github/workflows"


class WorkflowContractTests(unittest.TestCase):
    def setUp(self):
        workflow = WORKFLOWS / "ci.yml"
        if not workflow.exists():
            workflow = WORKFLOWS / "build.yml"
        self.ci = workflow.read_text()

    def test_events_and_metadata_cannot_replace_application_verification(self):
        self.assertNotRegex(self.ci, r"(?m)^  push:")
        self.assertRegex(self.ci, r"(?m)^  merge_group:")
        self.assertRegex(self.ci, r"(?m)^  workflow_dispatch:")
        condition = "github.event.action == 'edited' && !github.event.changes.base"
        self.assertIn(condition + " && 'metadata' || 'verification'", self.ci)
        self.assertIn(condition + " && 'PR metadata'", self.ci)

    def test_every_application_step_is_gated(self):
        lightweight = {"Classify changes", "Test CI routing",
                       "Validate workflows and release metadata"}
        steps = re.split(r"\n      - ", "\n" + self.ci.split("    steps:\n", 1)[1])[1:]
        self.assertGreater(len(steps), 3)
        for step in steps:
            first_line = step.splitlines()[0]
            if first_line.startswith("uses: actions/checkout@"):
                continue
            if first_line.removeprefix("name: ") in lightweight:
                continue
            with self.subTest(step=first_line):
                self.assertIn("steps.changes.outputs.heavy == 'true'", step)

    def test_contribution_workflow_has_no_application_setup(self):
        path = WORKFLOWS / "contributions.yml"
        if not path.exists():
            return
        contribution = path.read_text()
        self.assertIn("edited", contribution)
        self.assertNotIn("outputs.heavy", contribution)
        self.assertNotRegex(contribution, r"setup-go|setup-java|setup-gradle|go test|gradlew")

    def test_model_checks_keep_their_toolchains(self):
        if not (ROOT / "schema/contracts").is_dir():
            return
        for toolchain in ("actions/setup-go@", "actions/setup-java@", "gradle/actions/setup-gradle@"):
            self.assertIn(toolchain, self.ci)


if __name__ == "__main__":
    unittest.main()
