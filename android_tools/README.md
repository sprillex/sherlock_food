# Sprillex Android Build Automation API - Developer Guide

This document serves as the integration manual for app development teams building remote mobile clients or web dashboards for the Sprillex Android Build Automation system.

---

## 1. Authentication & Headers

All HTTP requests to the Build Automation API require Bearer Token authorization.

### Request Headers
```http
Authorization: Bearer <YOUR_API_KEY>
Content-Type: application/json
```

---

## 2. API Endpoints Reference

### 2.1 Trigger Repository Clone & Build
Clones an Android Git repository into the build environment and runs its initial build.

- **HTTP Method**: `POST`
- **Path**: `/clone`
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

---

### 2.2 Trigger Project Update & Build
Fetches remote changes for an existing project, checks out the specified branch, and compiles the debug APK.

- **HTTP Method**: `POST`
- **Path**: `/update`
- **Request Body Options**:
  - Build default main/master branch:
    ```json
    {
      "project": "reviewassistant",
      "branch_flag": "-m"
    }
    ```
  - Build branch with newest commit:
    ```json
    {
      "project": "reviewassistant",
      "branch_flag": "-n"
    }
    ```
  - Build specific branch:
    ```json
    {
      "project": "reviewassistant",
      "branch_flag": "feature/remote-api"
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

---

### 2.3 Poll Job Status
Polls the execution state of a background clone or update build job.

- **HTTP Method**: `GET`
- **Path**: `/status/{job_id}`

#### Response Examples:
- **Running / In Progress** (`HTTP 200 OK`):
  ```json
  {
    "job_id": "c16194b5-6548-43bb-8a60-a29d5b78f447",
    "status": "running",
    "project": "reviewassistant",
    "output": "Job queued..."
  }
  ```
- **Success** (`HTTP 200 OK`):
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
- **Failed** (`HTTP 200 OK`):
  ```json
  {
    "job_id": "c16194b5-6548-43bb-8a60-a29d5b78f447",
    "status": "failed",
    "project": "reviewassistant",
    "output": "Compilation failed..."
  }
  ```

---

### 2.4 Query Build Execution Logs
Retrieves the raw output log of a specific build job.

- **HTTP Method**: `GET`
- **Path**: `/logs/{job_id}`
- **Response**: `text/plain`

---

### 2.5 List All Cloned Projects
Retrieves all managed Android projects and indicates if compiled APKs are present.

- **HTTP Method**: `GET`
- **Path**: `/projects`
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

---

### 2.6 List Project APKs
Lists all compiled APK files for a given project sorted by newest creation time first.

- **HTTP Method**: `GET`
- **Path**: `/projects/{project}/apks`
- **Response** (`HTTP 200 OK`):
  ```json
  {
    "project": "reviewassistant",
    "apks": [
      {
        "filename": "Reviewassistant-B2026-08-26-23-18.apk",
        "size_bytes": 15420112,
        "created_at": 1787702280.0,
        "nas_relative_path": "projects/reviewassistant/apk/Reviewassistant-B2026-08-26-23-18.apk",
        "download_url": "/download/reviewassistant/Reviewassistant-B2026-08-26-23-18.apk"
      }
    ]
  }
  ```

---

### 2.7 Download APK Binary Directly
Downloads or installs an APK file over HTTP.

- **HTTP Method**: `GET`
- **Path**: `/download/{project}/{filename}`
- **Response Header**: `Content-Type: application/vnd.android.package-archive`

---

## 3. Storage & Delivery Integration

Mobile clients can access and install built APKs using two complementary mechanisms:

### 3.1 NAS SMB Share Storage Layout
When `SECONDARY_APK_DIR` is set on the server, compiled APKs are saved to the NAS share under:
```
<NAS_SMB_ROOT>/projects/<PROJECT_NAME>/apk/<APK_FILENAME>
```
The `nas_path` field returned in `/status/{job_id}` and `/projects/{project}/apks` contains the exact relative path for mobile client SMB indexers.

### 3.2 HTTP Direct Streaming
Mobile clients can also directly download and trigger in-app package installation via:
```
http://<SERVER_IP>:<PORT>/download/<project>/<filename>
```
with the `Authorization: Bearer <API_KEY>` header.
