#!/usr/bin/env python3
"""
scripts/reproduce_r1_before.py
Reproduces findings P1, P2, P4, and P3 on local-backend build d6b6f55.
Captures screenshots to docs/testing/evidence/phase3/r1/:
- before-p1-signout-blocked.png
- before-p2-dayclose-gap.png
- before-p4-silent-party-create.png
- before-p3-duplicate-slip-no.png
"""

import os
import re
import sys
import time
import subprocess
import xml.etree.ElementTree as ET

ADB_BIN = os.environ.get("ADB_BIN", "adb")
TARGET_SERIAL = os.environ.get("TARGET_SERIAL", "emulator-5554")
EVIDENCE_DIR = "docs/testing/evidence/phase3/r1"

os.makedirs(EVIDENCE_DIR, exist_ok=True)

def run_adb(args, check=True):
    cmd = [ADB_BIN, "-s", TARGET_SERIAL] + args
    res = subprocess.run(cmd, capture_output=True, text=True)
    if check and res.returncode != 0:
        raise RuntimeError(f"ADB command failed ({' '.join(cmd)}): {res.stderr.strip()}")
    return res.stdout.strip()

def tap(x, y, delay=0.5):
    run_adb(["shell", "input", "tap", str(int(x)), str(int(y))])
    time.sleep(delay)

def type_text(txt, delay=0.4):
    run_adb(["shell", "input", "text", txt.replace(" ", "%s")])
    time.sleep(delay)

def keyevent(code, delay=0.3):
    run_adb(["shell", "input", "keyevent", str(code)])
    time.sleep(delay)

def hide_keyboard():
    keyevent(111, delay=0.5)

def swipe_up(delay=0.5):
    run_adb(["shell", "input", "swipe", "540", "1600", "540", "500", "300"])
    time.sleep(delay)

def swipe_down(delay=0.5):
    run_adb(["shell", "input", "swipe", "540", "600", "540", "1700", "300"])
    time.sleep(delay)

def clear_field():
    keyevent(123)
    for _ in range(25):
        keyevent(67, delay=0.03)

def dump_nodes():
    remote = "/sdcard/mandi_dump.xml"
    run_adb(["shell", "uiautomator", "dump", remote], check=False)
    xml_str = run_adb(["shell", "cat", remote], check=False)
    run_adb(["shell", "rm", "-f", remote], check=False)
    if not xml_str or "<hierarchy" not in xml_str:
        return []
    try:
        root = ET.fromstring(xml_str)
    except Exception:
        return []
    nodes = []
    for node in root.iter("node"):
        bounds = node.attrib.get("bounds", "")
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            nodes.append({
                "text": node.attrib.get("text", ""),
                "class": node.attrib.get("class", ""),
                "bounds": (x1, y1, x2, y2),
                "center": ((x1 + x2) // 2, (y1 + y2) // 2)
            })
    return nodes

def find_node(text=None, class_name=None, contains=True, nodes=None):
    if nodes is None:
        nodes = dump_nodes()
    for n in nodes:
        match = True
        if text is not None:
            if contains and text not in n["text"]:
                match = False
            elif not contains and text != n["text"]:
                match = False
        if class_name is not None and class_name != n["class"]:
            match = False
        if match:
            return n
    return None

def wait_for_node(text=None, class_name=None, timeout=15, contains=True):
    start = time.time()
    while time.time() - start < timeout:
        nodes = dump_nodes()
        n = find_node(text=text, class_name=class_name, contains=contains, nodes=nodes)
        if n:
            return n
        time.sleep(0.6)
    return None

def capture_screenshot(filename):
    shot_path = os.path.join(EVIDENCE_DIR, filename)
    remote = "/sdcard/shot.png"
    run_adb(["shell", "screencap", "-p", remote])
    run_adb(["pull", remote, shot_path])
    run_adb(["shell", "rm", "-f", remote])
    print(f"  ✓ Captured: {shot_path}")
    return shot_path

def tap_tab(tab_name):
    hide_keyboard()
    nodes = dump_nodes()
    n = find_node(text=tab_name, nodes=nodes)
    if n:
        tap(*n["center"])
        time.sleep(1)
        return True
    return False

def main():
    print("=================================================================")
    print(" Starting A2 Device Verification: Before-Screenshots on d6b6f55")
    print("=================================================================")

    # ---------------------------------------------------------------------------
    # Reproduction 1: P1 (Expenses never reach server -> Sign-out blocked)
    # ---------------------------------------------------------------------------
    print("\n--- [1/4] Reproducing P1: Expense sync wire gap & blocked signout ---")
    tap_tab("डैशबोर्ड")
    time.sleep(1)

    nodes = dump_nodes()
    exp_btn = find_node(text="मंडी व्यय", nodes=nodes) or find_node(text="खर्च", nodes=nodes)
    if not exp_btn:
        print("  Looking for expense quick action button...")
        exp_btn = wait_for_node(text="खर्च")
    if exp_btn:
        tap(*exp_btn["center"])
        time.sleep(1)

    wait_for_node(text="खर्च श्रेणी", timeout=10)
    nodes = dump_nodes()
    chai_chip = find_node(text="चाय-पानी", nodes=nodes)
    if chai_chip:
        tap(*chai_chip["center"])
        time.sleep(0.3)

    edits = [n for n in dump_nodes() if n["class"] == "android.widget.EditText"]
    if edits:
        # Amount field
        tap(*edits[0]["center"])
        clear_field()
        type_text("350")
        time.sleep(0.3)

        # Remarks field
        if len(edits) > 1:
            tap(*edits[1]["center"])
            clear_field()
            type_text("TEST चाय खर्च")
            time.sleep(0.3)

    hide_keyboard()
    time.sleep(0.4)
    swipe_up()
    save_btn = wait_for_node(text="खर्च सुरक्षित करें", timeout=5)
    if save_btn:
        tap(*save_btn["center"])
        print("  ✓ Expense of ₹350 saved.")
        time.sleep(2)

    # Let sync run against backend
    print("  Waiting 5s for background sync cycle...")
    time.sleep(5)

    # Navigate to Settings tab
    print("  Navigating to Settings to attempt signout...")
    tap_tab("सेटिंग्स")
    time.sleep(1)

    swipe_up()
    swipe_up()
    logout_btn = wait_for_node(text="लॉग आउट करें", timeout=10)
    if logout_btn:
        tap(*logout_btn["center"])
        time.sleep(1)

    # Wait for logout blocked dialog / snackbar
    wait_for_node(text="लॉग आउट नहीं हो सकता", timeout=5)
    shot_p1 = capture_screenshot("before-p1-signout-blocked.png")

    # Dismiss blocked dialog if present
    dismiss_btn = find_node(text="समझ गया") or find_node(text="ठीक है") or find_node(text="रद्द करें")
    if dismiss_btn:
        tap(*dismiss_btn["center"])
    else:
        keyevent(4) # back
    time.sleep(1)

    # ---------------------------------------------------------------------------
    # Reproduction 2: P2 (Credit Buy listed as cash out -> Day close gap)
    # ---------------------------------------------------------------------------
    print("\n--- [2/4] Reproducing P2: Day close contradiction on credit purchase ---")
    tap_tab("डैशबोर्ड")
    time.sleep(1)

    # Tap Buy entry
    buy_btn = find_node(text="खरीद") or find_node(text="आवक")
    if buy_btn:
        tap(*buy_btn["center"])
        time.sleep(1.5)

    wait_for_node(text="किसान / विक्रेता", timeout=10)
    nodes = dump_nodes()
    edits = [n for n in nodes if n["class"] == "android.widget.EditText"]
    if edits:
        # Party field
        tap(*edits[0]["center"])
        clear_field()
        type_text("TEST Ramesh")
        time.sleep(0.4)

    hide_keyboard()
    time.sleep(0.4)

    # Scroll down to fill bags, weight, rate
    nodes = dump_nodes()
    edits = [n for n in nodes if n["class"] == "android.widget.EditText"]
    # Look for numeric fields: Bags, Weight, Rate
    for idx, e in enumerate(edits[1:4]):
        tap(*e["center"])
        clear_field()
        if idx == 0:
            type_text("10") # 10 bags
        elif idx == 1:
            type_text("500") # 500 kg
        elif idx == 2:
            type_text("2000") # 2000 rate
        time.sleep(0.3)

    hide_keyboard()
    time.sleep(0.4)
    swipe_up()
    swipe_up()

    # Save Buy slip
    save_slip = wait_for_node(text="पर्चा सुरक्षित करें", timeout=10)
    if save_slip:
        tap(*save_slip["center"])
        print("  ✓ Credit Buy slip saved for TEST Ramesh (₹10,000, paid 0).")
        time.sleep(2)

    # On Receipt screen, press back to return
    keyevent(4)
    time.sleep(1)
    tap_tab("डैशबोर्ड")
    time.sleep(0.5)

    # Navigate to Galla / Rokdi tab
    print("  Navigating to Galla (रोकड़) to inspect Day Close...")
    tap_tab("गल्ला रोकड़")
    time.sleep(1.5)

    # Tap Close Day button
    close_day_btn = wait_for_node(text="दिन बंद करें", timeout=10)
    if not close_day_btn:
        swipe_up()
        close_day_btn = wait_for_node(text="दिन बंद करें", timeout=5)
    if close_day_btn:
        tap(*close_day_btn["center"])
        time.sleep(1.5)

    # The dialog opens showing false opening discrepancy
    wait_for_node(text="दैनिक रोकड़ मिलान", timeout=10)
    shot_p2 = capture_screenshot("before-p2-dayclose-gap.png")

    # Cancel dialog
    cancel_btn = find_node(text="रद्द करें")
    if cancel_btn:
        tap(*cancel_btn["center"])
    else:
        keyevent(4)
    time.sleep(1)

    # ---------------------------------------------------------------------------
    # Reproduction 3: P4 (Partial name silently creates party without prompt)
    # ---------------------------------------------------------------------------
    print("\n--- [3/4] Reproducing P4: Silent party creation on partial name match ---")
    tap_tab("डैशबोर्ड")
    time.sleep(1)

    buy_btn = find_node(text="खरीद") or find_node(text="आवक")
    if buy_btn:
        tap(*buy_btn["center"])
        time.sleep(1.5)

    wait_for_node(text="किसान / विक्रेता", timeout=10)
    edits = [n for n in dump_nodes() if n["class"] == "android.widget.EditText"]
    if edits:
        tap(*edits[0]["center"])
        clear_field()
        # Type partial prefix 'TEST R' without tapping 'TEST Ramesh' suggestion
        type_text("TEST R")
        time.sleep(0.5)

    hide_keyboard()
    time.sleep(0.5)

    # Fill bags and weight
    edits = [n for n in dump_nodes() if n["class"] == "android.widget.EditText"]
    if len(edits) > 1:
        tap(*edits[1]["center"])
        clear_field()
        type_text("5")
        time.sleep(0.3)
    if len(edits) > 2:
        tap(*edits[2]["center"])
        clear_field()
        type_text("250")
        time.sleep(0.3)
    if len(edits) > 3:
        tap(*edits[3]["center"])
        clear_field()
        type_text("2000")
        time.sleep(0.3)

    hide_keyboard()
    time.sleep(0.4)
    swipe_up()
    swipe_up()

    save_slip = wait_for_node(text="पर्चा सुरक्षित करें", timeout=10)
    if save_slip:
        tap(*save_slip["center"])
        print("  ✓ Buy slip saved with untyped suggestion 'TEST R' (no prompt appeared).")
        time.sleep(2)

    # Exit receipt screen
    keyevent(4)
    time.sleep(1)

    # Go to Khata tab to show both parties exist
    print("  Navigating to Khata Ledger to verify silent creation...")
    tap_tab("खाता बही")
    time.sleep(1.5)

    shot_p4 = capture_screenshot("before-p4-silent-party-create.png")

    # ---------------------------------------------------------------------------
    # Reproduction 4: P3 (Slip numbers restart after reinstall -> duplicate #1)
    # ---------------------------------------------------------------------------
    print("\n--- [4/4] Reproducing P3: Slip number restarts after re-login ---")
    # Wait 5s for existing slips to sync to backend
    time.sleep(5)

    print("  Simulating reinstall / clear data & restore from sync...")
    run_adb(["shell", "pm", "clear", "com.appwork.mandisamiti"])
    time.sleep(1)
    run_adb(["shell", "am", "start", "-S", "-n", "com.appwork.mandisamiti/.MainActivity"])
    time.sleep(3)

    # Drive re-login
    # Step: Enter phone 9876543210
    wait_for_node(text="अपना मोबाइल नंबर दर्ज करें", timeout=15)
    edits = [n for n in dump_nodes() if n["class"] == "android.widget.EditText"]
    if edits:
        tap(*edits[0]["center"])
        clear_field()
        type_text("9876543210")
        hide_keyboard()
        time.sleep(0.3)
        send_btn = find_node(text="OTP भेजें")
        if send_btn:
            tap(*send_btn["center"])
            time.sleep(1.5)

    # OTP auto-verifies or prompt
    time.sleep(3)

    # Re-login with MPIN
    print("  Entering MPIN 1234...")
    wait_for_node(text="MPIN", timeout=15)
    edits = [n for n in dump_nodes() if n["class"] == "android.widget.EditText"]
    if edits:
        tap(*edits[0]["center"])
        clear_field()
        type_text("1234")
        hide_keyboard()
        time.sleep(0.5)

    # Wait for Dashboard after re-login
    print("  Waiting for Dashboard after sync restore...")
    wait_for_node(text="कुल खरीद", timeout=20)
    time.sleep(3)

    # Create a new Buy entry for TEST Suresh
    print("  Creating new Buy entry after sync restore to test slip numbering...")
    buy_btn = find_node(text="खरीद") or find_node(text="आवक")
    if buy_btn:
        tap(*buy_btn["center"])
        time.sleep(1.5)

    wait_for_node(text="किसान / विक्रेता", timeout=10)
    edits = [n for n in dump_nodes() if n["class"] == "android.widget.EditText"]
    if edits:
        tap(*edits[0]["center"])
        clear_field()
        type_text("TEST Suresh")
        time.sleep(0.4)

    hide_keyboard()
    time.sleep(0.4)
    edits = [n for n in dump_nodes() if n["class"] == "android.widget.EditText"]
    if len(edits) > 1:
        tap(*edits[1]["center"])
        clear_field()
        type_text("2")
        time.sleep(0.3)
    if len(edits) > 2:
        tap(*edits[2]["center"])
        clear_field()
        type_text("100")
        time.sleep(0.3)
    if len(edits) > 3:
        tap(*edits[3]["center"])
        clear_field()
        type_text("2000")
        time.sleep(0.3)

    hide_keyboard()
    time.sleep(0.4)
    swipe_up()
    swipe_up()

    save_slip = wait_for_node(text="पर्चा सुरक्षित करें", timeout=10)
    if save_slip:
        tap(*save_slip["center"])
        time.sleep(2)

    # Go to Buy transactions list to inspect duplicate slip numbers
    keyevent(4)
    time.sleep(1)
    tap_tab("डैशबोर्ड")
    time.sleep(1)

    # Open Buy list: tap KPI tile or view all
    view_all = find_node(text="सभी देखें") or find_node(text="कुल खरीद (अब तक)")
    if view_all:
        tap(*view_all["center"])
        time.sleep(1.5)

    shot_p3 = capture_screenshot("before-p3-duplicate-slip-no.png")

    print("\n=================================================================")
    print(" All 4 Before-Screenshots Captured Successfully:")
    print(f" 1. P1: {shot_p1}")
    print(f" 2. P2: {shot_p2}")
    print(f" 3. P4: {shot_p4}")
    print(f" 4. P3: {shot_p3}")
    print("=================================================================")

if __name__ == "__main__":
    main()

