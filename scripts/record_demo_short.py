#!/usr/bin/env python3
import subprocess
import time
import os
import sys

def run_adb(cmd):
    full_cmd = f"adb {cmd}"
    return subprocess.run(full_cmd, shell=True, capture_output=True, text=True)

def tap(x, y, delay=0.5):
    print(f"👉 Tap: ({x}, {y})")
    run_adb(f"shell input tap {x} {y}")
    time.sleep(delay)

def swipe(x1, y1, x2, y2, duration_ms=400, delay=0.5):
    print(f"👆 Swipe: ({x1}, {y1}) -> ({x2}, {y2}) [{duration_ms}ms]")
    run_adb(f"shell input swipe {x1} {y1} {x2} {y2} {duration_ms}")
    time.sleep(delay)

def main():
    print("🎬 Starting MandiSamiti Demo Short Recording Pipeline...")
    
    # 1. Clean previous recordings on device
    run_adb("shell rm -f /sdcard/demo_recording.mp4")
    
    # 2. Launch screenrecord in background
    print("🎥 Starting screenrecord on device (34s)...")
    rec_proc = subprocess.Popen(
        ["adb", "shell", "screenrecord", "--size", "1080x2160", "--bit-rate", "8000000", "--time-limit", "34", "/sdcard/demo_recording.mp4"]
    )
    
    time.sleep(1.5) # Initial settle
    
    # Timeline choreography matching voiceover:
    # 0s - 2.5s: Hook intro on Deal Entry screen
    print("⏳ Showing Deal Entry Screen & Tutorial Pill...")
    time.sleep(2.0)
    
    # Tap Tutorial Pill
    tap(300, 360, delay=2.5) # Opens MandiTutorialSheet
    
    # Tutorial Sheet is shown: Video player + 3 offline steps
    print("📖 Tutorial Sheet displayed (Reachable to fingers)")
    time.sleep(2.5)
    
    # Close Tutorial Sheet via close button
    tap(885, 210, delay=1.5)
    
    # Step 1: Boori count (50)
    print("🌾 Entering Boori count: 50")
    tap(200, 1220, delay=0.8) # Focus Boori
    tap(391, 1754, delay=0.6) # '5'
    tap(391, 2103, delay=1.0) # '0'
    
    # Step 2: Gross Weight (25.5 Qtl)
    print("⚖️ Entering Gross Weight: 25.5 Qtl")
    tap(500, 1220, delay=0.8) # Focus Gross Weight
    tap(391, 1577, delay=0.5) # '2'
    tap(391, 1754, delay=0.5) # '5'
    tap(643, 2103, delay=0.5) # '.'
    tap(391, 1754, delay=1.2) # '5' -> net weight updates to 25.15 Qtl!
    
    # Step 3: Switch to Stage 2
    print("📈 Switching to Stage 2: Auction & Rate")
    tap(820, 525, delay=1.0) # Stage 2 tab
    swipe(540, 1100, 540, 500, duration_ms=350, delay=0.8) # Scroll to reveal rate
    
    # Step 4: Rate Entry (2450)
    print("💰 Entering Rate: 2450")
    tap(250, 1020, delay=0.8) # Focus Rate
    tap(391, 1577, delay=0.5) # '2'
    tap(139, 1754, delay=0.5) # '4'
    tap(391, 1754, delay=0.5) # '5'
    tap(391, 2103, delay=1.2) # '0' -> live math updates to ₹61,617.50!
    
    # Step 5: Save Deal (Submit via Keypad ✓ पूर्ण)
    print("✅ Submitting Deal...")
    tap(910, 1930, delay=1.5)
    
    # Wait for screenrecord process to finish
    print("⏳ Waiting for recording to finalize...")
    rec_proc.wait()
    time.sleep(1.0)
    
    # Pull recording
    dest_video = "/Users/appworx/Desktop/Vivek-K/projects/MandiSamiti/assets/videos/demo_recording.mp4"
    os.makedirs(os.path.dirname(dest_video), exist_ok=True)
    print(f"📥 Pulling recording to {dest_video}...")
    run_adb(f"pull /sdcard/demo_recording.mp4 {dest_video}")
    
    if os.path.exists(dest_video) and os.path.getsize(dest_video) > 10000:
        print(f"🎉 Raw video captured successfully! Size: {os.path.getsize(dest_video)} bytes")
    else:
        print("❌ Recording pull failed or file empty.")
        sys.exit(1)

if __name__ == "__main__":
    main()
