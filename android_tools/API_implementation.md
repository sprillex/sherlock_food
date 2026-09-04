# Sprillex Android Build Automation API Implementation Plan

## 1. System Overview & Objectives

The Android Build Automation API is a lightweight, asynchronous HTTP service designed to enable remote Android app compilation, management, and distribution from mobile devices. It completely eliminates the requirement for direct host-to-device USB or TCP ADB connections.

### Key Objectives:
- **Remote Mobile Workflow**: Allow remote clients (mobile devices) to trigger Android repository clones, updates, and builds securely via REST endpoints over HTTP/HTTPS.
- **Asynchronous Execution**: Decouple build requests from HTTP connection lifecycles using FastAPI `BackgroundTasks` and UUID job tracking.
- **NAS & Direct Storage Delivery**: Built APKs are automatically organized and placed onto host storage (NAS SMB share) located at `${SECONDARY_APK_DIR}/projects/${PROJECT_NAME}/apk/${NEW_FILENAME}`.
- **HTTP APK Distribution**: Provide dedicated GET endpoints to list projects, list APK files per project, stream APK downloads directly over HTTP, and fetch detailed execution logs.
- **ADB Bypass**: Enhance script automation (`clone_android.sh` and generated `update.sh`) to detect headless/remote build environments and cleanly skip ADB installation steps without failure.
- **Dynamic Configuration**: Ensure all file system paths, NAS mount points, ports, and secret keys are dynamically configured through environment files (`.env`) and standard system variables.

---

## 2. Configuration & Environment Setup

### 2.1 Dynamic Environment Variables
The API daemon and generated scripts will read configuration from an environment file located at `/opt/builder-api/.env` or passed via systemd `EnvironmentFile`:

```bash
# Server Configuration
API_HOST=0.0.0.0
API_PORT=8080
API_KEY=CHANGE_ME_TO_A_SECURE_RANDOM_STRING

# Path Configurations
SPRILLEX_ANDROID_HOME=/home/james/sprillex/android
ANDROID_TOOLS_DIR=/opt/julebot/bin
SECONDARY_APK_DIR=/mnt/nas/apks

# Build Settings
SKIP_ADB_INSTALL=true
MAX_LOG_BYTES=4000
BUILD_TIMEOUT_SECONDS=900
```

### 2.2 Systemd Service Definition (`/etc/systemd/system/android-builder.service`)

```ini
[Unit]
Description=Sprillex Android Build Automation FastAPI Service
After=network.target

[Service]
Type=simple
User=julebot
Group=julebot
WorkingDirectory=/opt/builder-api
EnvironmentFile=/opt/builder-api/.env
ExecStart=/opt/builder-api/venv/bin/uvicorn server:app --host ${API_HOST} --port ${API_PORT} --workers 1
Restart=always
RestartSec=5

# Security Hardening
PrivateTmp=true
ProtectSystem=full
NoNewPrivileges=true

[Install]
WantedBy=multi-user.target
```

---

## 3. REST API Specification

All endpoints require Bearer Token authorization:
`Authorization: Bearer <API_KEY>`

### 3.1 Trigger Clone & Build
- **Endpoint**: `POST /clone`
- **Request Body**:
  ```json
  {
    "repo": "https://github.com/sprillex/reviewassistant.git"
  }
  ```
- **Response** (`HTTP 202 Accepted`):
  ```json
  {
    "job_id": "c16194b5-6548-43bb-8a60-a29d5b78f447",
    "status": "queued",
    "project": "reviewassistant"
  }
  ```

### 3.2 Trigger Update & Build
- **Endpoint**: `POST /update`
- **Request Body**:
  ```json
  {
    "project": "reviewassistant",
    "branch_flag": "-m"
  }
  ```
- **Response** (`HTTP 202 Accepted`):
  ```json
  {
    "job_id": "8f3b92a1-1234-4567-89ab-cdef01234567",
    "status": "queued",
    "project": "reviewassistant"
  }
  ```

### 3.3 Poll Job Status
- **Endpoint**: `GET /status/{job_id}`
- **Response** (`HTTP 200 OK` - In Progress):
  ```json
  {
    "job_id": "c16194b5-6548-43bb-8a60-a29d5b78f447",
    "status": "running",
    "project": "reviewassistant",
    "output": "Building APK..."
  }
  ```
- **Response** (`HTTP 200 OK` - Success):
  ```json
  {
    "job_id": "c16194b5-6548-43bb-8a60-a29d5b78f447",
    "status": "success",
    "project": "reviewassistant",
    "apk_filename": "Reviewassistant-B2026-08-26-23-18.apk",
    "nas_path": "projects/reviewassistant/apk/Reviewassistant-B2026-08-26-23-18.apk",
    "download_url": "/download/reviewassistant/Reviewassistant-B2026-08-26-23-18.apk",
    "output": "BUILD SUCCESSFUL in 42s\n✅ Build complete..."
  }
  ```

### 3.4 Query Build Logs
- **Endpoint**: `GET /logs/{job_id}`
- **Response** (`HTTP 200 OK`): Full raw execution stdout/stderr text log.

### 3.5 List Projects
- **Endpoint**: `GET /projects`
- **Response** (`HTTP 200 OK`):
  ```json
  {
    "projects": [
      {
        "name": "reviewassistant",
        "has_builds": true,
        "latest_apk": "Reviewassistant-B2026-08-26-23-18.apk"
      }
    ]
  }
  ```

### 3.6 List Project APKs
- **Endpoint**: `GET /projects/{project}/apks`
- **Response** (`HTTP 200 OK`):
  ```json
  {
    "project": "reviewassistant",
    "apks": [
      {
        "filename": "Reviewassistant-B2026-08-26-23-18.apk",
        "size_bytes": 15420112,
        "created_at": "2026-08-26T23:18:00Z",
        "nas_relative_path": "projects/reviewassistant/apk/Reviewassistant-B2026-08-26-23-18.apk",
        "download_url": "/download/reviewassistant/Reviewassistant-B2026-08-26-23-18.apk"
      }
    ]
  }
  ```

### 3.7 Download APK Direct Stream
- **Endpoint**: `GET /download/{project}/{filename}`
- **Response**: File stream (`application/vnd.android.package-archive`) with appropriate `Content-Disposition` header.

---

## 4. Supporting System & Script Modifications

### 4.1 ADB Bypass Logic (`clone_android.sh` & `update.sh`)
Update the ADB installation section of `update.sh` generated by `clone_android.sh` to support dynamic environment override and headless server operation:

```bash
# Check if ADB installation should be bypassed
if [ "${SKIP_ADB_INSTALL:-false}" = "true" ]; then
    echo "ℹ️  SKIP_ADB_INSTALL is active. Skipping ADB device installation."
elif adb get-state 1>/dev/null 2>&1; then
    adb install -r "$FINAL_PATH"
    echo "✅ Installed $NEW_FILENAME via ADB"
else
    echo "⚠️  ADB device not found. Output APK saved to: $FINAL_PATH"
fi
```

### 4.2 Dynamic Script Locations
Update `clone_android.sh` template to dynamically honor `$SECONDARY_APK_DIR` and `$BASE_DIR` from environment variables, ensuring compatibility with customizable NAS mounts.

---

## 5. FastAPI Implementation Blueprint (`server.py`)

```python
import os
import re
import subprocess
import uuid
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

def sanitize_input(val: str) -> bool:
    return bool(re.match(r"^[a-zA-Z0-9_./:-]+$", val))

def authenticate(authorization: Optional[str]):
    if not authorization or authorization != f"Bearer {API_KEY}":
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
        output = res.stdout + "\n" + res.stderr
        if res.returncode == 0:
            # Parse produced APK name from output
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
    if not sanitize_input(payload.repo):
        raise HTTPException(status_code=400, detail="Invalid repo name or URL.")

    repo_name = payload.repo.rstrip("/").split("/")[-1]
    if repo_name.endswith(".git"):
        repo_name = repo_name[:-4]

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
    if not sanitize_input(payload.project):
        raise HTTPException(status_code=400, detail="Invalid project name.")

    update_script = os.path.join(BASE_DIR, payload.project, "update.sh")
    if not os.path.exists(update_script):
        raise HTTPException(status_code=404, detail=f"Update script not found for project '{payload.project}'")

    cmd = [update_script]
    if payload.branch_flag:
        if not sanitize_input(payload.branch_flag):
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
    if not sanitize_input(project):
        raise HTTPException(status_code=400, detail="Invalid project name")

    builds_dir = os.path.join(BASE_DIR, project, "builds")
    if not os.path.exists(builds_dir):
        raise HTTPException(status_code=404, detail="Project or builds directory not found")

    apks = []
    for fname in os.listdir(builds_dir):
        if fname.endswith(".apk"):
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
    if not sanitize_input(project) or not sanitize_input(filename):
        raise HTTPException(status_code=400, detail="Invalid path parameters")

    apk_path = os.path.join(BASE_DIR, project, "builds", filename)
    if not os.path.exists(apk_path):
        raise HTTPException(status_code=404, detail="APK file not found")

    return FileResponse(
        path=apk_path,
        media_type="application/vnd.android.package-archive",
        filename=filename
    )
```

---

## 6. Phased Implementation Roadmap

When proceeding to implementation, execution will be divided into the following isolated steps:

### Phase 1: Script Enhancements
1. Update `android_tools/clone_android.sh` to include `SKIP_ADB_INSTALL` environment variable support.
2. Update the `update.sh` generation template inside `clone_android.sh` to handle headless environments without failing on missing ADB devices.

### Phase 2: FastAPI Service Implementation
1. Create the application directory structures and virtual environment setup files.
2. Implement `server.py` with authentication, job execution, status polling, project discovery, and download streaming endpoints.
3. Add configuration management via `.env` file parsing.

### Phase 3: Service Deployment & Integration
1. Configure systemd unit file (`android-builder.service`).
2. Verify systemd service startup, port binding, and permissions (`julebot` user).
3. End-to-end testing of POST `/clone`, POST `/update`, GET `/status`, and GET `/download` endpoints.
