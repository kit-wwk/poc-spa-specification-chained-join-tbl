---
name: check-issues
description: Check GitHub issues in this repo and fix them or ask for clarification
disable-model-invocation: true
allowed-tools: Bash(gh *) Read Grep Glob Edit Write Agent
---

# Check GitHub Issues

Check for open issues in this repository and take action on them.

## Steps

1. List all open issues:
   ```
   gh issue list --state open
   ```

2. If there are no open issues, report that the repo is clean and stop.

3. If there are open issues, for each issue:
   - Read the full issue details: `gh issue view <number>`
   - Read any comments: `gh api repos/{owner}/{repo}/pulls/<number>/comments` or `gh issue view <number> --comments`
   - Assess whether the issue is actionable:
     - **Question**: If the issue is a question about the codebase, architecture, or how something works — investigate the code, then comment on the issue with a clear, detailed answer. Reference specific files and lines where relevant.
     - **Clear bug or enhancement**: Investigate the codebase, implement a fix, run `./mvnw test` to verify, then commit and push. Comment on the issue summarizing what was done.
     - **Needs clarification**: Comment on the issue asking specific questions.
     - **Already fixed**: Comment noting it's resolved and close the issue.

4. After processing all issues, provide a summary of actions taken.

## Guidelines

- Always run `./mvnw test` before committing any fix to ensure nothing is broken.
- Keep fixes focused — only change what the issue requires.
- When commenting on issues, be concise and reference specific files/lines changed.
