```python
# Define the markdown content for the ADB Wireless Debugging and Vector Asset Guide
md_content = """# Android Developer Guide: ADB Wireless Debugging & Vector Asset Centering

This document serves as a reference for troubleshooting common issues with ADB Wireless Debugging connections and fixing alignment problems in Android Vector Drawables.

---

## 1. ADB Wireless Debugging Troubleshooting

### Issue 1: Protocol Fault on Initial Pairing
**Symptom:**

```

```text
File saved successfully as adb_and_vector_guide.md

```bash
$ adb pair 192.168.0.100:36871
Enter pairing code: 490877
* daemon not running; starting now at tcp:5037
* daemon started successfully
error: protocol fault (couldn't read status message): No error

```

**Cause:**
The ADB daemon server was not running when the command was issued. ADB attempted to start the background server (`tcp:5037`) and handle the pairing handshake simultaneously. The server did not initialize fast enough to catch the pairing response, resulting in a dropped handshake packet ("protocol fault").

**Resolution:**
Since the daemon is now successfully running in the background, simply re-run the pairing command:

```bash
adb pair <IP_ADDRESS>:<PAIRING_PORT>

```

If the server remains unresponsive, reset it completely:

```bash
adb kill-server
adb start-server
adb pair <IP_ADDRESS>:<PAIRING_PORT>

```

---

### Issue 2: "Target Machine Actively Refused It" on Connect

**Symptom:**

```bash
$ adb pair 192.168.0.100:37239
Enter pairing code: 091062
Successfully paired to 192.168.0.100:37239

$ adb connect 192.168.0.100:37239
cannot connect to 192.168.0.100:37239: No connection could be made because the target machine actively refused it. (10061)

```

**Cause:**
You attempted to connect to the **Pairing Port** (`37239`). In Android's wireless debugging protocol, the pairing port is a dynamic, temporary socket used solely for the 6-digit PIN handshake. Once pairing succeeds, this port immediately shuts down and actively refuses subsequent connections.

**Resolution:**

1. Keep the **Wireless Debugging** settings open on your Android device.
2. Locate the **IP address & Port** section on the primary screen (directly under the main toggle switch). This port is distinct from the pairing port.
3. Run the connection command utilizing this primary device port:
```bash
adb connect <IP_ADDRESS>:<DEVICE_PORT>

```


4. Verify the active state of the connection:
```bash
adb devices

```



> **Note for MINGW64 / Git Bash Users:** Git Bash occasionally encounters terminal emulation issues with interactive prompts (such as PIN entry input). If the prompt freezes during `adb pair`, execute the pairing command inside a standard Windows Command Prompt (`cmd`) or PowerShell, then resume development inside Git Bash.

---

## 2. Android Vector Drawable Scaling & Centering

### Issue: Off-Center Group Scaling

When manually scaling paths within a `<group>` tag inside an Android VectorDrawable, the asset can shift toward the top-left corner, destroying layout symmetry.

### Mathematical Correction

By default, Android VectorDrawables apply scaling transformations relative to the origin coordinate system `(0,0)` at the top-left corner. If a group is scaled down by a factor $S$, the visual area contracts, leaving an asymmetrical gap. To recenter the shifted content within a square canvas, the translation vector must follow this formula:

$$\text{Translation Vector} = \frac{\text{Viewport Size} \times (1 - \text{Scale Factor})}{2}$$

For a standard $24 \times 24$ viewport scaled down to $60\\%$ (`0.6`):

$$\text{Translation} = \frac{24 \times (1 - 0.6)}{2} = \frac{24 \times 0.4}{2} = 4.8$$

### Implementation Layouts

#### Approach A: Manual Translation (Calculated)

This approach explicitly transforms coordinates to maintain a predictable matrix behavior across older layout engines.

```xml
<vector xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <group
        android:scaleX="0.6"
        android:scaleY="0.6"
        android:translateX="4.8"
        android:translateY="4.8">
        <path
            android:fillColor="#E8520A"
            android:pathData="M12,2L4,5V11C4,16.55 7.84,21.74 12,23C16.16,21.74 20,16.55 20,11V5L12,2Z" />
        <path
            android:fillColor="#FFFFFF"
            android:pathData="M12,5L14.5,9.5L13,11L15,13L12,18L9,13L11,11L9.5,9.5L12,5Z" />
    </group>
</vector>

```

#### Approach B: Pivot-Point Scaling (Recommended)

By explicitly declaring pivot coordinates at the exact geographic midpoint of the viewport (`12, 12`), the vector engine scales directly from the center. This eliminates the need for manual translation variables.

```xml
<vector xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <group
        android:scaleX="0.6"
        android:scaleY="0.6"
        android:pivotX="12"
        android:pivotY="12">
        <path
            android:fillColor="#E8520A"
            android:pathData="M12,2L4,5V11C4,16.55 7.84,21.74 12,23C16.16,21.74 20,16.55 20,11V5L12,2Z" />
        <path
            android:fillColor="#FFFFFF"
            android:pathData="M12,5L14.5,9.5L13,11L15,13L12,18L9,13L11,11L9.5,9.5L12,5Z" />
    </group>
</vector>

```

"""

# Write the content to a markdown file

output_path = "adb_and_vector_guide.md"
with open(output_path, "w") as f:
f.write(md_content)

print(f"File saved successfully as {output_path}")

```
Your Markdown file is ready
[file-tag: code-generated-file-0-1780132649766646690]

I have consolidated both topics into a clean, well-formatted `.md` file titled `adb_and_vector_guide.md`. It includes:

1. **ADB Wireless Debugging Fixes**: Breakdowns of the `protocol fault` error and the `10061 connection refused` error (explaining the difference between pairing ports vs device ports).
2. **Vector Drawable Centering**: The mathematical formula explaining why the layout was shifting, along with corrected XML blocks for both manual translation calculations and the cleaner pivot-point (`pivotX`/`pivotY`) approach.

You can drop this directly into your repository or project documentation folder!

```
