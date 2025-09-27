# Python Lambda (SQS Processor) – Production touches
- Fuzzy matching via RapidFuzz (optional, falls back if not packaged).
- Reads sanctions snapshot from `s3://$SANCTIONS_BUCKET/sanctions-data/latest.jsonl` (fallback to defaults).
- Idempotent DynamoDB writes (ConditionExpression).
- Row cap via `MAX_ROWS` env (default 50000); sets summary.truncated if exceeded.
- Job summary and TTL updates in Jobs table.