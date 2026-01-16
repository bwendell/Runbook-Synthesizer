---
title: High Memory Troubleshooting
tags: [memory, linux, oom]
applicable_shapes: ["VM.*", "BM.*"]
severity_triggers: [WARNING, CRITICAL]
---

# High Memory Troubleshooting

## Prerequisites
- SSH access to the host
- sudo privileges

## Step 1: Check Current Memory Usage
Run the following to see memory breakdown:
```bash
free -h
cat /proc/meminfo | head -20
```

## Step 2: Identify Top Memory Consumers
```bash
ps aux --sort=-%mem | head -20
```

## Step 3: Check for OOM Events
```bash
dmesg | grep -i "out of memory" | tail -10
journalctl -k | grep -i oom
```

## Step 4: Check Swap Usage
```bash
swapon --show
vmstat 1 5
```

## Step 5: Analyze Memory-Mapped Files
```bash
pmap -x $(pidof <process_name>) | head -30
```

## Resolution Options
1. Restart memory-intensive processes
2. Increase swap space
3. Scale up instance shape
4. Review application memory configuration
