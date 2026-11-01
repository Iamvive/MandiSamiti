#!/usr/bin/env python3
"""
MandiSamiti Automated Onboarding to Dashboard Driver
Drives fresh install through Phone -> OTP -> New Shop -> Dashboard.
Never prints or logs the OTP_STATIC_CODE or any secret.
"""

import os
import re
import sys
import time
import subprocess
import xml.etree.ElementTree as ET

ADB_BIN = os.environ.get("ADB_BIN", "adb")
TARGET_SERIAL = os.environ.get("TARGET_SERIAL", "emulator-5554")
OTP_STATIC_CODE = os.environ.get("OTP_STATIC_CODE", "")

def run_adb(args, check=True):
    cmd = [ADB_BIN, "-s", TARGET_SERIAL] + args
    res = subprocess.run(cmd, capture_output=True, text=True)
    if check and res.returncode != 0:
        clean_cmd = ["***" if (OTP_STATIC_CODE and OTP_STATIC_CODE in c) else c for c in cmd]
        stderr = res.stderr.strip()
        if OTP_STATIC_CODE:
            stderr = stderr.replace(OTP_STATIC_CODE, "***")
        raise RuntimeError(f"ADB command failed ({' '.join(clean_cmd)}): {stderr}")
    return res.stdout.strip()

def tap(x, y, delay=0.6):
    run_adb(["shell", "input", "tap", str(int(x)), str(int(y))])
    time.sleep(delay)

def input_text(text, delay=0.5):
    escaped = text.replace(" ", "%s")
    run_adb(["shell", "input", "text", escaped])
    time.sleep(delay)

def keyevent(code, delay=0.3):
    run_adb(["shell", "input", "keyevent", str(code)])
    time.sleep(delay)

def hide_keyboard():
    keyevent(111, delay=0.5)

def clear_field():
    keyevent(123)  # KEYCODE_MOVE_END
    for _ in range(15):
        keyevent(67, delay=0.04)  # KEYCODE_DEL

def dump_tree():
    remote = "/sdcard/mandi_tree_dump.xml"
    run_adb(["shell", "uiautomator", "dump", remote], check=False)
    xml_str = run_adb(["shell", "cat", remote], check=False)
    run_adb(["shell", "rm", "-f", remote], check=False)
    if not xml_str or "<hierarchy" not in xml_str:
        return None
    try:
        return ET.fromstring(xml_str)
    except Exception:
        return None

def find_node(tree, text=None, class_name=None, contains=True):
    if tree is None:
        return None
    for node in tree.iter("node"):
        n_text = node.attrib.get("text", "")
        n_class = node.attrib.get("class", "")
        match = True
        if text is not None:
            if contains and text not in n_text:
                match = False
            elif not contains and text != n_text:
                match = False
        if class_name is not None and n_class != class_name:
            match = False
        if match:
            bounds = node.attrib.get("bounds", "")
            m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
            if m:
                x1, y1, x2, y2 = map(int, m.groups())
                return {
                    "text": n_text,
                    "class": n_class,
                    "bounds": (x1, y1, x2, y2),
                    "center": ((x1 + x2) // 2, (y1 + y2) // 2)
                }
    return None

def wait_for_node(text=None, class_name=None, timeout=20, contains=True):
    start = time.time()
    while time.time() - start < timeout:
        tree = dump_tree()
        node = find_node(tree, text=text, class_name=class_name, contains=contains)
        if node:
            return node
        time.sleep(0.8)
    return None

def find_all_edit_texts(tree):
    if tree is None:
        return []
    items = []
    for node in tree.iter("node"):
        if node.attrib.get("class") == "android.widget.EditText":
            bounds = node.attrib.get("bounds", "")
            m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
            if m:
                x1, y1, x2, y2 = map(int, m.groups())
                items.append({
                    "text": node.attrib.get("text", ""),
                    "bounds": (x1, y1, x2, y2),
                    "center": ((x1 + x2) // 2, (y1 + y2) // 2)
                })
    items.sort(key=lambda item: item["bounds"][1])
    return items

def swipe_up():
    run_adb(["shell", "input", "swipe", "540", "1600", "540", "800", "300"])
    time.sleep(0.6)

def main():
    if not TARGET_SERIAL.startswith("emulator-"):
        print(f"FATAL: Target serial '{TARGET_SERIAL}' is not an emulator! Refusing to run.", file=sys.stderr)
        sys.exit(1)

    if not OTP_STATIC_CODE:
        print("FATAL: OTP_STATIC_CODE environment variable is required.", file=sys.stderr)
        sys.exit(1)

    print(f"[*] Driving fresh onboarding on {TARGET_SERIAL}...")

    # Step 1: Wait for Phone screen
    print("[1/4] Waiting for Phone entry screen...")
    phone_title = wait_for_node(text="अपना मोबाइल नंबर दर्ज करें", timeout=25)
    if not phone_title:
        # Check if already on New Shop or Dashboard
        tree = dump_tree()
        if find_node(tree, text="दुकान व फर्म का विवरण"):
            print("  ✓ Already on New Shop screen.")
        elif find_node(tree, text="कुल खरीद"):
            print("  ✓ Already on Dashboard.")
            return
        else:
            raise RuntimeError("Phone entry screen did not appear within timeout.")

    if phone_title:
        tree = dump_tree()
        edit_texts = find_all_edit_texts(tree)
        if not edit_texts:
            raise RuntimeError("Could not find phone number EditText field.")

        tap(*edit_texts[0]["center"])
        clear_field()
        input_text("9876543210")
        hide_keyboard()
        time.sleep(0.5)

        tree = dump_tree()
        send_btn = find_node(tree, text="OTP भेजें")
        if not send_btn:
            raise RuntimeError("Could not find 'OTP भेजें' button.")

        tap(*send_btn["center"])
        print("  ✓ Phone submitted.")

    # Step 2: Wait for OTP screen
    print("[2/4] Waiting for OTP verification screen...")
    tree = dump_tree()
    # Check if already progressed to New Shop
    if not find_node(tree, text="दुकान व फर्म का विवरण"):
        otp_action = wait_for_node(text="OTP सत्यापित करें", timeout=20)
        if not otp_action:
            tree = dump_tree()
            if not find_node(tree, text="दुकान व फर्म का विवरण"):
                raise RuntimeError("OTP verification screen did not appear within timeout.")

        tree = dump_tree()
        otp_fields = find_all_edit_texts(tree)
        if otp_fields and not find_node(tree, text="दुकान व फर्म का विवरण"):
            tap(*otp_fields[0]["center"])
            clear_field()
            # Input secret OTP code directly without logging
            input_text(OTP_STATIC_CODE)
            hide_keyboard()
            time.sleep(0.5)

            tree = dump_tree()
            verify_btn = find_node(tree, text="OTP सत्यापित करें")
            if verify_btn:
                tap(*verify_btn["center"])
                print("  ✓ OTP submitted.")
            else:
                print("  ✓ OTP auto-verified.")
    else:
        print("  ✓ Progressed to New Shop screen.")

    # Step 3: Wait for New Shop screen
    print("[3/4] Waiting for New Shop details screen...")
    shop_title = wait_for_node(text="दुकान व फर्म का विवरण", timeout=20)
    if not shop_title:
        shop_title = wait_for_node(text="दुकान बनाएँ", timeout=10)
    if not shop_title:
        raise RuntimeError("New Shop screen did not appear within timeout.")

    time.sleep(0.5)
    tree = dump_tree()
    edit_texts = find_all_edit_texts(tree)
    if len(edit_texts) < 3:
        raise RuntimeError(f"Expected at least 3 shop input fields, found {len(edit_texts)}")

    # Field 0: Shop name
    tap(*edit_texts[0]["center"])
    clear_field()
    input_text("TEST Shop")

    # Field 1: Owner name
    tap(*edit_texts[1]["center"])
    clear_field()
    input_text("TEST Trader")

    # Field 2: Mandi name
    tap(*edit_texts[2]["center"])
    clear_field()
    input_text("TEST Mandi")

    # If MPIN fields are already in view:
    if len(edit_texts) >= 5:
        mpin_f = edit_texts[3]
        confirm_f = edit_texts[4]
    else:
        hide_keyboard()
        swipe_up()
        tree = dump_tree()
        edit_texts = find_all_edit_texts(tree)
        mpin_f = edit_texts[-2]
        confirm_f = edit_texts[-1]

    # Field 3: MPIN
    tap(*mpin_f["center"])
    clear_field()
    input_text("1234")

    # Field 4: Confirm MPIN
    tap(*confirm_f["center"])
    clear_field()
    input_text("1234")

    hide_keyboard()
    time.sleep(0.5)

    # Scroll down to ensure "दुकान बनाएँ" button is visible and tap it
    swipe_up()
    time.sleep(0.5)

    tree = dump_tree()
    create_shop_btn = find_node(tree, text="दुकान बनाएँ")
    if not create_shop_btn:
        tap(540, 2200)
    else:
        tap(*create_shop_btn["center"])
    print("  ✓ Shop profile and MPIN submitted.")

    # Step 4: Verify Dashboard reached
    print("[4/4] Verifying Dashboard is reached...")
    dash_node = wait_for_node(text="कुल खरीद", timeout=25)
    if not dash_node:
        dash_node = wait_for_node(text="डैशबोर्ड", timeout=10)
    if not dash_node:
        dash_node = wait_for_node(text="रोकड़", timeout=10)

    if not dash_node:
        raise RuntimeError("Failed to reach Dashboard after shop registration!")

    print("  ✓ Live Dashboard reached successfully!")

    # Take screenshot of Dashboard
    evidence_dir = "docs/testing/evidence/phase3/setup"
    os.makedirs(evidence_dir, exist_ok=True)
    shot_path = os.path.join(evidence_dir, "01-dashboard-fresh-install.png")
    run_adb(["shell", "screencap", "-p", "/sdcard/dash_shot.png"])
    run_adb(["pull", "/sdcard/dash_shot.png", shot_path])
    run_adb(["shell", "rm", "-f", "/sdcard/dash_shot.png"])
    print(f"  ✓ Evidence screenshot captured: {shot_path}")

if __name__ == "__main__":
    main()
