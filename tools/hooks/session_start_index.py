"""SessionStart context for Claude Code: hand the agent the knowledge index before the first prompt.

A committed script rather than an inline one-liner, so it can be read, reviewed and tested.
"""

import json
import os
import sys


def main():
    root = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
    try:
        with open(os.path.join(root, "conductor", "knowledge", "INDEX.md"), encoding="utf-8") as handle:
            index = handle.read()
    except OSError:
        return 0
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "SessionStart",
                                             "additionalContext": "[conductor/knowledge/INDEX.md]\n" + index}}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
