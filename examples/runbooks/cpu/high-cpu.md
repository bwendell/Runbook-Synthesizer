---
title: High CPU Troubleshooting
tags: [cpu, linux, performance]
applicable_shapes: ["VM.*", "BM.*"]
severity_triggers: [WARNING, CRITICAL]
---

# High CPU Troubleshooting

## Prerequisites
- SSH access to the host
- sudo privileges

## Step 1: Check CPU Usage Overview
```bash
top -bn1 | head -20
uptime
```

## Step 2: Identify Top CPU Consumers
```bash
ps aux --sort=-%cpu | head -20
```

## Step 3: Check Load Average Trend
```bash
sar -u 1 10
mpstat -P ALL 1 5
```

## Step 4: Check for Runaway Processes
```bash
pidstat -u 1 10
```

## Step 5: Analyze Process Threads
```bash
ps -eLf | grep <process_name>
top -H -p <pid>
```

## Step 6: Check for I/O Wait
```bash
iostat -x 1 5
vmstat 1 10
```

## Resolution Options
1. Identify and restart problematic process
2. Review application configuration
3. Scale up instance shape
4. Add more vCPUs
