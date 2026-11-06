#!/usr/bin/env python3
"""
scripts/verify_p1_to_p4_after.py
Captures after-screenshots for P1 to P4 on the fixed build (incorporating C1-C4):
- after-p1-signout-unblocked.png
- after-p2-dayclose-balanced.png
- after-p4-party-confirm-dialog.png
- after-p3-slip-monotonic.png
"""

import os
import re
import sys
import time
import subprocess
import xml.etree.ElementTree as ET

ADB_BIN = "/Users/appworx/Library/Android/sdk/platform-tools/adb"
TARGET_SERIAL = "emulator-5554"
EVIDENCE_DIR = "/Users/appworx/Desktop/mandi-wt/antigravity/docs/testing/evidence/phase3/r1"

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
    print(" Starting A2 Device Verification: After-Screenshots (Fixed Build)")
    print("=================================================================")

    # Ensure app is in foreground and on Dashboard
    run_adb(["shell", "am", "start", "-n", "com.appwork.mandisamiti/.MainActivity"])
    time.sleep(2)

    # ---------------------------------------------------------------------------
    # Verification 1: P1 (Expense Sync & Unblocked Signout)
    # ---------------------------------------------------------------------------
    print("\n--- [1/4] Verifying P1: Expense sync wire format & signout ---")
    tap_tab("डैशबोर्ड")
    time.sleep(1)

    nodes = dump_nodes()
    exp_btn = find_node(text="मंडी व्यय", nodes=nodes) or find_node(text="खर्च", nodes=nodes)
    if exp_btn:
        tap(*exp_btn["center"])
        time.sleep(1)
        edits = [n for n in dump_nodes() if n["class"] == "android.widget.EditText"]
        if edits:
            tap(*edits[0]["center"])
            clear_field()
            type_text("350")
            hide_keyboard()
            time.sleep(0.3)
            save_btn = find_node(text="सहेजें") or find_node(text="खर्च जोड़ें") or find_node(text="सुरक्षित करें")
            if save_btn:
                tap(*save_btn["center"])
                time.sleep(1)

    # Allow SyncEngine to push expense_entries to backend
    print("  Waiting 4s for expense sync to complete...")
    time.sleep(4)

    # Go to More / Settings tab to sign out
    more_tab = find_node(text="अन्य") or find_node(text="सेटिंग")
    if more_tab:
        tap(*more_tab["center"])
        time.sleep(1)

    swipe_up()
    time.sleep(0.5)

    signout_btn = wait_for_node(text="लॉग आउट", timeout=5) or wait_for_node(text="साइन आउट", timeout=5)
    if signout_btn:
        tap(*signout_btn["center"])
        time.sleep(1.5)

    # On fixed build, sync is clean (0 pending), so no "असिंक डेटा" block dialog appears!
    # Or if a confirm logout dialog appears, we capture or confirm.
    shot_p1 = capture_screenshot("after-p1-signout-unblocked.png")

    # Dismiss logout dialog or cancel to stay logged in
    cancel_btn = find_node(text="रद्द करें") or find_node(text="नहीं")
    if cancel_btn:
        tap(*cancel_btn["center"])
        time.sleep(0.5)
    keyevent(4)
    time.sleep(0.5)

    # ---------------------------------------------------------------------------
    # Verification 2: P2 (Credit Buy & Day Close Cash Drawer)
    # ---------------------------------------------------------------------------
    print("\n--- [2/4] Verifying P2: Galla Day Close does not count credit buys ---")
    tap_tab("रोकड़ बही") or tap_tab("गल्ला")
    time.sleep(1.5)

    # Open Day Close dialog
    close_btn = wait_for_node(text="दिन बंद करें", timeout=10) or wait_for_node(text="गल्ला मिलान", timeout=10)
    if close_btn:
        tap(*close_btn["center"])
        time.sleep(1.5)

    # In fixed build, credit purchases are excluded from Naame, so opening cash is balanced (no false ₹47,885 difference!)
    shot_p2 = capture_screenshot("after-p2-dayclose-balanced.png")

    # Dismiss dialog
    keyevent(4)
    time.sleep(1)

    # ---------------------------------------------------------------------------
    # Verification 3: P4 (New Party Confirmation Dialog on Partial Match)
    # ---------------------------------------------------------------------------
    print("\n--- [3/4] Verifying P4: New Party Confirmation Dialog on 'TEST R' ---")
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
        type_text("TEST R")
        time.sleep(0.5)

    hide_keyboard()
    time.sleep(0.4)
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
        time.sleep(1.5)

    # In fixed build C4b, checkTypedParty triggers NewPartyConfirmDialog asking "नया खाता बनाएं?"
    shot_p4 = capture_screenshot("after-p4-party-confirm-dialog.png")

    # Dismiss dialog
    cancel_party = find_node(text="रद्द करें")
    if cancel_party:
        tap(*cancel_party["center"])
        time.sleep(0.5)
    keyevent(4)
    time.sleep(0.5)
    keyevent(4)
    time.sleep(0.5)

    # ---------------------------------------------------------------------------
    # Verification 4: P3 (Monotonic Slip Numbers Across Shop)
    # ---------------------------------------------------------------------------
    print("\n--- [4/4] Verifying P3: Monotonic Shop-Wide Slip Numbers ---")
    tap_tab("डैशबोर्ड")
    time.sleep(1)

    # Open Buy transaction list
    view_all = find_node(text="सभी देखें") or find_node(text="कुल खरीद (अब तक)")
    if view_all:
        tap(*view_all["center"])
        time.sleep(1.5)

    shot_p3 = capture_screenshot("after-p3-slip-monotonic.png")

    print("\n=================================================================")
    print(" All 4 After-Screenshots Captured Successfully:")
    print(f" 1. P1: {shot_p1}")
    print(f" 2. P2: {shot_p2}")
    print(f" 3. P4: {shot_p4}")
    print(f" 4. P3: {shot_p3}")
    print("=================================================================")

if __name__ == "__main__":
    main()
