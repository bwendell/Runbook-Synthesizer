---
title: CPU Troubleshooting Guide
tags:
  - cpu
  - performance
  - linux
applicable_shapes:
  - VM.Standard.*
  - BM.Standard.*
---

# CPU Troubleshooting Guide

This guide helps troubleshoot CPU-related issues on Linux systems.

## Identifying High CPU Usage

When CPU usage is consistently high, application performance suffers.

### Symptoms

- Slow response times
- Process queuing
- High load average

### Diagnostic Commands

Run the following commands to diagnose CPU issues:

```bash
top -b -n 1
mpstat -P ALL 1 5
pidstat 1 5
```

## Resolving CPU Issues

Once you've identified the cause, follow these steps.

### Identify Resource-Heavy Processes

Find processes consuming the most CPU:

```bash
ps aux --sort=-%cpu | head -20
top -o %CPU
```

### Check for Runaway Processes

Look for stuck or infinite loop processes:

```bash
strace -p <PID> -o trace.log
timeout 30 strace -p <PID>
```

### Adjust Process Priority

Lower priority of non-critical processes:

```bash
renice +10 -p <PID>
```

## Prevention

Set up monitoring to catch issues early.

### CPU Alerts

Configure CloudWatch alarms for CPU thresholds above 80%.

```bash
aws cloudwatch put-metric-alarm \
  --alarm-name HighCPUUtilization \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold
```
