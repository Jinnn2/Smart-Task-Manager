
# Smart Study Task Manager

A **JavaFX desktop study assistant** designed for students to plan, schedule, and analyze study tasks.
The system combines a **drag-and-drop weekly calendar**, **risk-aware task analytics**, and an **LLM-ready assistant API**, while remaining fully functional offline with local persistence.

---

## ✨ Key Features

### 📅 Visual Scheduling
<img width="1900" height="702" alt="image" src="https://github.com/user-attachments/assets/6f177969-5360-403e-9470-5d6c327783f3" />

* Weekly calendar with **compressed morning view (00:00–08:00)** and a **26-hour vertical span**
* Drag-and-drop task scheduling with **30-minute snapping**
* Real-time current-time indicator and clear 24:00 boundary

### 🧠 Task Modeling
<img width="791" height="367" alt="image" src="https://github.com/user-attachments/assets/c4358994-a394-4413-84e0-0632f37dee08" />

* Task attributes: priority, deadline, estimated duration, optional start time
* Automatic overdue detection and status update
* Postponement tracking (e.g. *“已推迟 × N”*)

### ⚠️ Risk & Analytics
<img width="919" height="154" alt="image" src="https://github.com/user-attachments/assets/d012bcda-c81d-4141-ba77-be5f437c7ca2" />

* High-risk task detection
* Upcoming-deadline highlighting
* Warning when scheduled end time exceeds deadline

### 💬 Natural-Language Assistant
<img width="1815" height="382" alt="image" src="https://github.com/user-attachments/assets/db0a88d0-da3b-4046-b68a-c2b04c862d43" />
<img width="707" height="347" alt="image" src="https://github.com/user-attachments/assets/fe221aad-1d22-4073-bc1c-a087ea3ffb5b" />

* **SET mode**: create or update tasks via natural language
* **CODE mode**: generate runnable Java tools (full source code required)
* Context-aware chat interface

  * `Enter` to send
  * `Shift + Enter` for newline

### 🧪 Local Tool Sandbox
<img width="305" height="515" alt="image" src="https://github.com/user-attachments/assets/f7ef6207-09b0-4690-b041-bd39b9f954f5" />

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
<img width="669" height="456" alt="image" src="https://github.com/user-attachments/assets/766168d6-d3ff-4381-a261-ab36d625318f" />

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
## Author
Jinnn2 金厚泽 北京航空航天大学
