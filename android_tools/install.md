# Sprillex Android Build Automation API - Installation Guide

This document provides step-by-step instructions for installing, configuring, and deploying the **Sprillex Android Build Automation API** daemon on a Linux build host.

---

## 1. Prerequisites

Before installing the service, ensure the build host meets the following requirements:

- **Operating System**: Linux (Ubuntu 20.04+, Debian 11+, RHEL 8+, or Fedora)
- **Python**: Python 3.9+ with `python3-venv` and `python3-pip` installed
- **Android SDK**: Android SDK and Java Development Kit (JDK 17+) configured on the host machine
- **Utilities**: `git`, `curl`, `bash`, `systemd`
- **User Account**: System service account (default: `julebot` or your host runner user)

---

## 2. Step-by-Step Installation

### Step 1: Create Application Directory & Set Permissions
Create the dedicated deployment directory at `/opt/builder-api` and assign ownership to the service user:

```bash
sudo mkdir -p /opt/builder-api
sudo chown -R julebot:julebot /opt/builder-api
cd /opt/builder-api
```

### Step 2: Copy API Files
Copy `server.py` and `requirements.txt` from the `android_tools` repository directory into `/opt/builder-api`:

```bash
cp /path/to/sprillex/android_tools/server.py /opt/builder-api/
cp /path/to/sprillex/android_tools/requirements.txt /opt/builder-api/
```

### Step 3: Prepare Python Virtual Environment
Create an isolated Python virtual environment and install required dependencies:

```bash
cd /opt/builder-api
python3 -m venv venv
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
```

### Step 4: Configure Environment Variables
Create the environment configuration file at `/opt/builder-api/.env` (using `builder-api.env.example` as a template):

```bash
cp /path/to/sprillex/android_tools/builder-api.env.example /opt/builder-api/.env
```

Edit `/opt/builder-api/.env` to configure your host parameters:

```bash
# Server Configuration
API_HOST=0.0.0.0
API_PORT=8080
API_KEY=GenerateASecureRandomKeyHere123!

# Path Configurations
SPRILLEX_ANDROID_HOME=/home/james/sprillex/android
ANDROID_TOOLS_DIR=/opt/julebot/bin
SECONDARY_APK_DIR=/mnt/nas/apks

# Build Settings
SKIP_ADB_INSTALL=true
MAX_LOG_BYTES=4000
BUILD_TIMEOUT_SECONDS=900
```

> **Note**: Secure the permissions of `.env` so only the service user can read it:
> ```bash
> chmod 600 /opt/builder-api/.env
> ```

---

## 3. Systemd Service Deployment

### Step 1: Install Systemd Service Unit
Copy the systemd unit file to `/etc/systemd/system/android-builder.service`:

```bash
sudo cp /path/to/sprillex/android_tools/android-builder.service /etc/systemd/system/
```

Verify `/etc/systemd/system/android-builder.service` contents:

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

### Step 2: Enable & Start Service
Reload systemd manager configuration, enable automatic startup, and start the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now android-builder.service
```

---

## 4. Verification & Health Check

### Check Systemd Status
Verify that the service is active and running without errors:

```bash
sudo systemctl status android-builder.service
```

### Test API Health Endpoint
Send an authenticated request to query the projects list:

```bash
curl -s -X GET "http://localhost:8080/projects" \
  -H "Authorization: Bearer GenerateASecureRandomKeyHere123!"
```

**Expected Response**:
```json
{
  "projects": []
}
```

---

## 5. Maintenance & Logs

- **View Live Logs**:
  ```bash
  journalctl -u android-builder.service -f
  ```
- **Restart Service**:
  ```bash
  sudo systemctl restart android-builder.service
  ```
- **Stop Service**:
  ```bash
  sudo systemctl stop android-builder.service
  ```
