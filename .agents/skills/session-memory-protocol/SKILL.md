---
name: session-memory-protocol
description: Use when start or work in a session. The Brain. Core protocols for reading, and maintaining the session's Memory
---

# Session Memory Protocol (The Brain)

This skill governs how the Agent (You) interacts with the project's long-term memory system and defines the core operational rules.

## 🧠 Session Memory Architecture

The **Session Memory** is your single source of truth about system state. It persists context between sessions. 

### Location
`.agents/session-memory/` (at Root of the project)

### Core Files
1.  
**`session-memory/core/current-state.md`**
: 🎯 
**THE NOW**
. What is happening, active phase, tasks.
2.
**`session-memory/core/progress.md`**
: 📊 
**THE HISTORY**
. What has been done.
3.  
**`session-memory/NOTES_NEXT_SESSION.md`**
: 📝 
**THE HANDOVER**
. Specific instructions for this session.

### Session Start Protocol (Reading Order)
When starting a task or session, you MUST read the core files in order to "boot up" your context.
