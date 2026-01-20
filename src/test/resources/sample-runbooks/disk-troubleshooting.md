---
title: Disk Space Troubleshooting Guide
tags:
  - disk
  - storage
  - linux
applicable_shapes:
  - VM.Standard.*
  - BM.Standard.*
---

# Disk Space Troubleshooting Guide

This guide helps troubleshoot disk space issues on Linux systems.

## Identifying Disk Space Issues

When disk space runs low, applications may fail to write data.

### Symptoms

- Write failures
- Application errors
- Log truncation
- Database failures

### Diagnostic Commands

Run the following commands to diagnose disk space issues:

```bash
df -h
du -sh /*
lsof +L1
```

## Resolving Disk Space Issues

Once you've identified the cause, follow these steps.

### Find Large Files

Identify the largest files consuming space:

```bash
find / -type f -size +100M -exec ls -lh {} \;
du -ah / | sort -rh | head -20
```

### Clean Up Log Files

Remove or rotate old log files:

```bash
journalctl --vacuum-time=7d
find /var/log -name "*.gz" -mtime +30 -delete
```

### Clean Package Cache

Remove unused packages and cache:

```bash
apt-get clean
yum clean all
dnf clean all
```

### Remove Orphaned Files

Find and remove temporary files:

```bash
find /tmp -type f -mtime +7 -delete
find /var/tmp -type f -mtime +30 -delete
```

## Prevention

Set up monitoring to catch issues early.

### Disk Alerts

Configure CloudWatch alarms for disk usage thresholds above 80%.

```bash
aws cloudwatch put-metric-alarm \
  --alarm-name HighDiskUtilization \
  --metric-name DiskSpaceUtilization \
  --namespace CWAgent \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold
```
