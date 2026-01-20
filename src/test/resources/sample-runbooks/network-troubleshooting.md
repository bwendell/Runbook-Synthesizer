---
title: Network Troubleshooting Guide
tags:
  - network
  - latency
  - connectivity
  - linux
applicable_shapes:
  - VM.Standard.*
  - BM.Standard.*
---

# Network Troubleshooting Guide

This guide helps troubleshoot network-related issues on Linux systems.

## Identifying Network Issues

When network issues occur, connectivity and latency problems arise.

### Symptoms

- Connection timeouts
- High latency
- Packet loss
- DNS resolution failures

### Diagnostic Commands

Run the following commands to diagnose network issues:

```bash
ping -c 10 8.8.8.8
traceroute 8.8.8.8
netstat -tuln
ss -tuln
```

## Resolving Network Issues

Once you've identified the cause, follow these steps.

### Check DNS Resolution

Verify DNS is working correctly:

```bash
nslookup example.com
dig example.com
cat /etc/resolv.conf
```

### Check Connectivity

Test connectivity to external services:

```bash
curl -v https://example.com
wget --spider https://example.com
nc -zv example.com 443
```

### Check Firewall Rules

Review firewall configuration:

```bash
iptables -L -n
firewall-cmd --list-all
ufw status verbose
```

### Check Network Interface Status

Verify interface configuration:

```bash
ip addr show
ip route show
ethtool eth0
```

## Prevention

Set up monitoring to catch issues early.

### Network Alerts

Configure CloudWatch alarms for network metrics:

```bash
aws cloudwatch put-metric-alarm \
  --alarm-name HighNetworkLatency \
  --metric-name NetworkLatency \
  --namespace CWAgent \
  --threshold 100 \
  --comparison-operator GreaterThanThreshold
```
