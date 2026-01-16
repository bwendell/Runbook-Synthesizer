---
title: GPU Errors Troubleshooting
tags: [gpu, nvidia, cuda]
applicable_shapes: ["GPU*", "BM.GPU*"]
severity_triggers: [WARNING, CRITICAL]
---

# GPU Errors Troubleshooting

## Prerequisites
- SSH access to the host
- sudo privileges
- NVIDIA drivers installed

## Step 1: Check GPU Status
```bash
nvidia-smi
nvidia-smi -q
```

## Step 2: Check GPU Memory Usage
```bash
nvidia-smi --query-gpu=memory.used,memory.free,memory.total --format=csv
```

## Step 3: Check GPU Temperature
```bash
nvidia-smi --query-gpu=temperature.gpu --format=csv,noheader
```

## Step 4: Check GPU Process Usage
```bash
nvidia-smi pmon -s u -d 1
```

## Step 5: Check for GPU Errors
```bash
nvidia-smi --query-gpu=ecc.errors.corrected.aggregate.total,ecc.errors.uncorrected.aggregate.total --format=csv
dmesg | grep -i nvidia | tail -20
```

## Step 6: Check CUDA Status
```bash
nvcc --version
python3 -c "import torch; print(torch.cuda.is_available())"
```

## Step 7: Check Driver Version
```bash
cat /proc/driver/nvidia/version
modinfo nvidia
```

## Resolution Options
1. Restart GPU workloads
2. Clear GPU memory
3. Update NVIDIA drivers
4. Check for hardware issues
