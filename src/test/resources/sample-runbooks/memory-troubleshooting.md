---
title: Memory Troubleshooting Guide
tags:
  - memory
  - oom
  - linux
applicable_shapes:
  - VM.Standard.*
  - BM.Standard.*
---

# Memory Troubleshooting Guide

This guide helps troubleshoot memory issues on Linux systems.

## Identifying High Memory Usage

When a system runs low on memory, performance degrades significantly.

### Symptoms

- Slow application response
- High swap usage
- OOM killer activity

### Diagnostic Commands

Run the following commands to diagnose memory issues:

```bash
free -h
cat /proc/meminfo
top -o %MEM
```

## Resolving Memory Issues

Once you've identified the cause, follow these steps.

### Clearing Cache

If the issue is cache buildup:

```bash
sync && echo 3 > /proc/sys/vm/drop_caches
```

### Restarting Services

Identify and restart memory-hungry services:

```bash
systemctl restart heavy-service
```

## Prevention

Set up monitoring to catch issues early.

### Memory Alerts

Configure CloudWatch alarms for memory thresholds above 80%.
