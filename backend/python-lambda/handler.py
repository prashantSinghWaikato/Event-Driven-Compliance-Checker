import json, csv, io, os, boto3, datetime, traceback, re

# optional fuzzy libs
try:
    from rapidfuzz import fuzz
except Exception:
    fuzz = None
try:
    from unidecode import unidecode
except Exception:
    def unidecode(x): return x

dynamodb = boto3.resource('dynamodb')
s3 = boto3.client('s3')

RESULTS_TABLE = os.getenv('DDB_TABLE', 'ComplianceResults')
JOBS_TABLE = os.getenv('JOBS_TABLE', 'ComplianceJobs')
SANCTIONS_BUCKET = os.getenv('SANCTIONS_BUCKET')  # optional; fallback to uploads bucket
MAX_ROWS = int(os.getenv('MAX_ROWS', '50000'))
TTL_DAYS = int(os.getenv('TTL_DAYS', '0'))

results = dynamodb.Table(RESULTS_TABLE)
jobs = dynamodb.Table(JOBS_TABLE)

def _now_iso(): return datetime.datetime.utcnow().isoformat()
def _ttl_epoch(): 
    if TTL_DAYS <= 0: return None
    return int((datetime.datetime.utcnow() + datetime.timedelta(days=TTL_DAYS)).timestamp())

def _norm(s: str) -> str:
    s = unidecode((s or "").lower())
    s = re.sub(r"[^a-z0-9\s]", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s

def _score(name: str, sanctioned: str) -> int:
    n, s = _norm(name), _norm(sanctioned)
    if not n or not s: return 0
    if fuzz:
        return int(0.7 * fuzz.token_sort_ratio(n, s) + 0.3 * fuzz.partial_ratio(n, s))
    # fallback naive
    if n == s: return 95
    if n in s or s in n: return 70
    return 20

def _load_sanctions(bucket: str):
    # expects JSONL with {"name": "..."} per line
    b = SANCTIONS_BUCKET or bucket
    key = "sanctions-data/latest.jsonl"
    try:
        obj = s3.get_object(Bucket=b, Key=key)
        lines = obj['Body'].read().decode('utf-8', errors='ignore').splitlines()
        names = []
        for ln in lines:
            try:
                j = json.loads(ln)
                nm = j.get('name')
                if nm: names.append(nm)
            except Exception:
                continue
        if names: 
            print(json.dumps({"level":"info","msg":"loaded sanctions snapshot","count":len(names),"bucket":b,"key":key}))
            return names
    except Exception as e:
        print(json.dumps({"level":"warn","msg":"no sanctions snapshot, using defaults","error":str(e)}))
    return ['ACME HOLDINGS', 'GLOBAL SERVICES LTD', 'FOO CONSORTIUM']

def _update_summary(job_id: str, total: int, high: int, medium: int, low: int, truncated: bool):
    expr = "SET #s=:s, updatedAt=:u, summary=:sum"
    vals = {
        ":s": "DONE",
        ":u": _now_iso(),
        ":sum": {
            "total": total, "high": high, "medium": medium, "low": low, "truncated": truncated
        }
    }
    names = {"#s": "status"}
    if TTL_DAYS > 0:
        expr += ", ttl=:ttl"
        vals[":ttl"] = _ttl_epoch()
    jobs.update_item(Key={"jobId": job_id}, UpdateExpression=expr,
                     ExpressionAttributeNames=names,
                     ExpressionAttributeValues={k: _to_av(v) for k,v in vals.items()})

def _set_status(job_id: str, status: str, error: str | None = None):
    expr = "SET #s=:s, updatedAt=:u"
    vals = {":s": status, ":u": _now_iso()}
    names = {"#s": "status"}
    if error:
        expr += ", #e=:e"
        vals[":e"] = error
        names["#e"] = "error"
    if TTL_DAYS > 0 and status in ("QUEUED","PROCESSING","FAILED"):
        expr += ", ttl=:ttl"
        vals[":ttl"] = _ttl_epoch()
    jobs.update_item(Key={"jobId": job_id}, UpdateExpression=expr,
                     ExpressionAttributeNames=names,
                     ExpressionAttributeValues={k: _to_av(v) for k,v in vals.items()})

def _to_av(v):
    if isinstance(v, dict):
        return {"M": {k:_to_av(x) for k,x in v.items()}}
    if isinstance(v, bool):
        return {"BOOL": v}
    if isinstance(v, (int,float)):
        return {"N": str(v)}
    if v is None:
        return {"NULL": True}
    return {"S": str(v)}

def handler(event, context):
    for rec in event.get('Records', []):
        body = json.loads(rec['body'])
        job_id = body['jobId']; bucket = body['bucket']; key = body['key']
        country_default = body.get('country') or 'NZ'
        try:
            _set_status(job_id, 'PROCESSING')
            sanctions = _load_sanctions(bucket)

            obj = s3.get_object(Bucket=bucket, Key=key)
            text = obj['Body'].read().decode('utf-8', errors='ignore')
            reader = csv.DictReader(io.StringIO(text))

            total = high = medium = low = 0
            truncated = False

            for i, row in enumerate(reader, start=1):
                if i > MAX_ROWS:
                    truncated = True
                    break
                name = (row.get('name') or '').strip()
                if not name:
                    continue
                total += 1
                best = max((( _score(name, s), s) for s in sanctions), key=lambda x: x[0])
                score = best[0]
                if score >= 80: high += 1
                elif score >= 50: medium += 1
                else: low += 1
                item = {
                    'jobId': job_id,
                    'recordId': str(i),
                    'name': name,
                    'country': row.get('country') or country_default,
                    'matchName': best[1],
                    'riskScore': score,
                    'processedAt': _now_iso(),
                }
                try:
                    results.put_item(Item=item,
                                     ConditionExpression="attribute_not_exists(recordId)")
                except Exception as e:
                    print(json.dumps({"level":"warn","msg":"duplicate recordId", "jobId":job_id, "recordId":i}))
                    continue

            _update_summary(job_id, total, high, medium, low, truncated)
        except Exception as e:
            err = f"{type(e).__name__}: {e}"
            print(json.dumps({"level":"error","msg":"processing failed","jobId":job_id,"error":err,"trace":traceback.format_exc()}))
            _set_status(job_id, 'FAILED', err)
    return { 'ok': True }