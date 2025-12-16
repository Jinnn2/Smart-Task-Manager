
# Smart Study Task Manager

A **JavaFX desktop study assistant** designed for students to plan, schedule, and analyze study tasks.
The system combines a **drag-and-drop weekly calendar**, **risk-aware task analytics**, and an **LLM-ready assistant API**, while remaining fully functional offline with local persistence.

---

## ✨ Key Features

### 📅 Visual Scheduling

* Weekly calendar with **compressed morning view (00:00–08:00)** and a **26-hour vertical span**
* Drag-and-drop task scheduling with **30-minute snapping**
* Real-time current-time indicator and clear 24:00 boundary

### 🧠 Task Modeling

* Task attributes: priority, deadline, estimated duration, optional start time
* Automatic overdue detection and status update
* Postponement tracking (e.g. *“已推迟 × N”*)

### ⚠️ Risk & Analytics

* High-risk task detection
* Upcoming-deadline highlighting
* Warning when scheduled end time exceeds deadline

### 💬 Natural-Language Assistant

* **SET mode**: create or update tasks via natural language
* **CODE mode**: generate runnable Java tools (full source code required)
* Context-aware chat interface

  * `Enter` to send
  * `Shift + Enter` for newline

### 🧪 Local Tool Sandbox

* Run any executable **JAR** placed under `Tools/`
* Supports both CLI and GUI applications
* Isolated execution with logging

### 💾 Persistence

* File-based storage using JSON
* User data and settings stored locally under `~/.smart-study`
* Fully usable **without network access**

---

## 🏗 Architecture Overview

Layered and extensible design, suitable for further research or feature expansion:

```
ui → controller → service → repository → model
                     ↓
                    api (LLM-ready)
```

---

## 📂 Project Structure

```
src/main/java/edu/study/
├─ ui/
│  └─ SmartTaskWidget.java        # JavaFX UI
├─ controller/
│  └─ TaskController.java         # Orchestration layer
├─ service/
│  └─ TaskService.java            # Core task logic
├─ api/
│  ├─ AssistantAPI.java           # LLM-facing abstraction
│  └─ impl/
│     ├─ OpenAIChatAssistantAPI.java
│     └─ OpenAIChatClient.java    # Chat + SET / CODE handling
├─ repository/                    # File / JSON persistence
├─ model/                         # Task, Priority, Status, etc.
└─ util/
   └─ ToolSandboxRunner.java      # Local JAR execution
```

```
Tools/    # Place runnable JAR tools here
logs/     # Chat, CODE generation, and sandbox logs
```

---

## 🔧 Prerequisites

* **JDK 17+**
* **Maven 3.8+**
* (Optional) OpenAI-compatible API endpoint for LLM features

---

## 🚀 Quick Start

### Run in development mode

```bash
mvn javafx:run
```

### Build executable package

```bash
mvn -DskipTests package
```

---

## 🤖 LLM Configuration (Optional)

Create a `.env` file or set environment variables:

```env
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.openai.com/v1   # or any compatible endpoint
OPENAI_MODEL=gpt-4o
```

### Assistant Behavior

* **Task operations** → return `SET:` followed by natural-language task description
* **Code generation** → return `CODE:` + `FileName.java` + complete Java source
  (compiled and packaged automatically into `Tools/`)

If no LLM is available, the system falls back to a **rule-based assistant**.

---

## 🧭 Usage Tips

* **Calendar**

  * Drag tasks to schedule
  * Right-click-drag to move existing blocks
  * Morning hours (0–8h) are visually compressed

* **Sandbox**

  * Click refresh to reload JARs from `Tools/`
  * GUI tools open in separate windows
  * Logs are captured automatically

* **Chat**

  * Context panel displays user profile and next task summary
  * Logs stored under `logs/`

---

## 📁 Data & Settings

* Tasks:

  ```
  ~/.smart-study/tasks.json
  ```
* User settings & profile:

  ```
  ~/.smart-study/settings.json
  ```

---

## 📝 Notes

* CODE generation debug logs:

  ```
  logs/code-debug.log
  ```
* Sandbox execution logs are visible both in UI and log files

---

## 📄 License

MIT License
(Adjust as needed)


我可以按你的目标继续细化。
