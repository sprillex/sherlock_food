import os
import re
import secrets
import subprocess
import uuid
from pathlib import Path
from typing import Dict, Optional, List
from fastapi import FastAPI, BackgroundTasks, Header, HTTPException, status
from fastapi.responses import FileResponse, PlainTextResponse
from pydantic import BaseModel

app = FastAPI(title="Sprillex Android Build Trigger API", version="1.0.0")

# --- Environment Configurations ---
API_KEY = os.getenv("API_KEY", "CHANGE_ME_TO_A_SECURE_RANDOM_STRING")
BASE_DIR = os.getenv("SPRILLEX_ANDROID_HOME", os.path.expanduser("~/sprillex/android"))
TOOLS_DIR = os.getenv("ANDROID_TOOLS_DIR", "/opt/julebot/bin")
SECONDARY_APK_DIR = os.getenv("SECONDARY_APK_DIR", "")
SKIP_ADB_INSTALL = os.getenv("SKIP_ADB_INSTALL", "true")

# In-memory job state tracker
jobs: Dict[str, Dict[str, str]] = {}

class CloneRequest(BaseModel):
    repo: str

class UpdateRequest(BaseModel):
    project: str
    branch_flag: Optional[str] = None

def sanitize_name(val: str) -> bool:
    """Disallow path separators and directory traversal sequences."""
    if not val or ".." in val or "/" in val or "\\" in val:
        return False
    return bool(re.match(r"^[a-zA-Z0-9_.-]+$", val))

def sanitize_branch(val: str) -> bool:
    """Allow standard git branch characters (including slashes) but disallow directory traversal."""
    if not val or ".." in val or "\\" in val:
        return False
    return bool(re.match(r"^[a-zA-Z0-9_./-]+$", val))

def sanitize_repo(val: str) -> bool:
    """Allow standard git URL characters but disallow directory traversal."""
    if not val or ".." in val:
        return False
    return bool(re.match(r"^[a-zA-Z0-9_./:-]+$", val))

def authenticate(authorization: Optional[str]):
    expected = f"Bearer {API_KEY}"
    if not authorization or not secrets.compare_digest(authorization, expected):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing Authorization header"
        )

def run_script_task(job_id: str, script_cmd: list, project_name: str):
    env = os.environ.copy()
    env["SKIP_ADB_INSTALL"] = SKIP_ADB_INSTALL
    if SECONDARY_APK_DIR:
        env["SECONDARY_APK_DIR"] = SECONDARY_APK_DIR

    try:
        res = subprocess.run(
            script_cmd,
            capture_output=True,
            text=True,
            timeout=900,
            env=env
        )
        output = (res.stdout or "") + "\n" + (res.stderr or "")
        if res.returncode == 0:
            apk_match = re.search(r"Renaming to: ([^\s]+\.apk)", output)
            apk_filename = apk_match.group(1) if apk_match else None

            nas_path = f"projects/{project_name}/apk/{apk_filename}" if apk_filename else None
            download_url = f"/download/{project_name}/{apk_filename}" if apk_filename else None

            jobs[job_id] = {
                "status": "success",
                "project": project_name,
                "apk_filename": apk_filename,
                "nas_path": nas_path,
                "download_url": download_url,
                "output": output[-4000:]
            }
        else:
            jobs[job_id] = {
                "status": "failed",
                "project": project_name,
                "output": output[-4000:]
            }
    except Exception as e:
        jobs[job_id] = {
            "status": "error",
            "project": project_name,
            "output": str(e)
        }

@app.post("/clone", status_code=status.HTTP_202_ACCEPTED)
def clone_and_build(
    payload: CloneRequest,
    background_tasks: BackgroundTasks,
    authorization: Optional[str] = Header(None)
):
    authenticate(authorization)
    if not sanitize_repo(payload.repo):
        raise HTTPException(status_code=400, detail="Invalid repo name or URL.")

    repo_name = payload.repo.rstrip("/").split("/")[-1]
    if repo_name.endswith(".git"):
        repo_name = repo_name[:-4]

    if not sanitize_name(repo_name):
        raise HTTPException(status_code=400, detail="Invalid project name derived from repo URL.")

    job_id = str(uuid.uuid4())
    jobs[job_id] = {"status": "running", "project": repo_name, "output": "Job queued..."}

    clone_script = os.path.join(TOOLS_DIR, "clone_android.sh")
    background_tasks.add_task(run_script_task, job_id, [clone_script, payload.repo], repo_name)
    return {"job_id": job_id, "status": "queued", "project": repo_name}

@app.post("/update", status_code=status.HTTP_202_ACCEPTED)
def update_and_build(
    payload: UpdateRequest,
    background_tasks: BackgroundTasks,
    authorization: Optional[str] = Header(None)
):
    authenticate(authorization)
    if not sanitize_name(payload.project):
        raise HTTPException(status_code=400, detail="Invalid project name.")

    update_script = os.path.realpath(os.path.join(BASE_DIR, payload.project, "update.sh"))
    expected_base = os.path.realpath(BASE_DIR)
    if not update_script.startswith(expected_base) or not os.path.exists(update_script):
        raise HTTPException(status_code=404, detail=f"Update script not found for project '{payload.project}'")

    cmd = [update_script]
    if payload.branch_flag:
        if not sanitize_branch(payload.branch_flag):
            raise HTTPException(status_code=400, detail="Invalid branch flag.")
        cmd.append(payload.branch_flag)

    job_id = str(uuid.uuid4())
    jobs[job_id] = {"status": "running", "project": payload.project, "output": "Job queued..."}

    background_tasks.add_task(run_script_task, job_id, cmd, payload.project)
    return {"job_id": job_id, "status": "queued", "project": payload.project}

@app.get("/status/{job_id}")
def get_job_status(job_id: str, authorization: Optional[str] = Header(None)):
    authenticate(authorization)
    if job_id not in jobs:
        raise HTTPException(status_code=404, detail="Job not found")
    return {"job_id": job_id, **jobs[job_id]}

@app.get("/logs/{job_id}", response_class=PlainTextResponse)
def get_job_logs(job_id: str, authorization: Optional[str] = Header(None)):
    authenticate(authorization)
    if job_id not in jobs:
        raise HTTPException(status_code=404, detail="Job not found")
    return jobs[job_id].get("output", "")

@app.get("/projects")
def list_projects(authorization: Optional[str] = Header(None)):
    authenticate(authorization)
    if not os.path.exists(BASE_DIR):
        return {"projects": []}

    result = []
    for item in os.listdir(BASE_DIR):
        proj_dir = os.path.join(BASE_DIR, item)
        if os.path.isdir(proj_dir):
            builds_dir = os.path.join(proj_dir, "builds")
            apks = []
            if os.path.exists(builds_dir):
                apks = [f for f in os.listdir(builds_dir) if f.endswith(".apk")]
            apks.sort(reverse=True)
            result.append({
                "name": item,
                "has_builds": len(apks) > 0,
                "latest_apk": apks[0] if apks else None
            })
    return {"projects": result}

@app.get("/projects/{project}/apks")
def list_project_apks(project: str, authorization: Optional[str] = Header(None)):
    authenticate(authorization)
    if not sanitize_name(project):
        raise HTTPException(status_code=400, detail="Invalid project name")

    builds_dir = os.path.realpath(os.path.join(BASE_DIR, project, "builds"))
    expected_base = os.path.realpath(BASE_DIR)
    if not builds_dir.startswith(expected_base) or not os.path.exists(builds_dir):
        raise HTTPException(status_code=404, detail="Project or builds directory not found")

    apks = []
    for fname in os.listdir(builds_dir):
        if fname.endswith(".apk") and sanitize_name(fname):
            fpath = os.path.join(builds_dir, fname)
            stat = os.stat(fpath)
            apks.append({
                "filename": fname,
                "size_bytes": stat.st_size,
                "created_at": stat.st_mtime,
                "nas_relative_path": f"projects/{project}/apk/{fname}",
                "download_url": f"/download/{project}/{fname}"
            })
    apks.sort(key=lambda x: x["created_at"], reverse=True)
    return {"project": project, "apks": apks}

@app.get("/download/{project}/{filename}")
def download_apk(project: str, filename: str, authorization: Optional[str] = Header(None)):
    authenticate(authorization)
    if not sanitize_name(project) or not sanitize_name(filename):
        raise HTTPException(status_code=400, detail="Invalid path parameters")

    builds_dir = os.path.realpath(os.path.join(BASE_DIR, project, "builds"))
    apk_path = os.path.realpath(os.path.join(builds_dir, filename))
    expected_base = os.path.realpath(BASE_DIR)

    if not apk_path.startswith(expected_base) or not os.path.exists(apk_path):
        raise HTTPException(status_code=404, detail="APK file not found")

    return FileResponse(
        path=apk_path,
        media_type="application/vnd.android.package-archive",
        filename=filename
    )
