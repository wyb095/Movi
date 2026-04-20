# Compact instructions
When compacting, preserve only:
- current batch and exact task
- files changed
- failing test names and assertion diffs
- current hypothesis
- next step

Drop:
- full command output
- repeated test runs
- old read file contents
- dead ends