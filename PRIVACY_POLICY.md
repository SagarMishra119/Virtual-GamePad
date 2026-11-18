# Privacy Policy for Virtual Gamepad

**Effective Date:** November 18, 2026  
**Application ID:** `com.virtualgamepad.app`  

Virtual Gamepad ("we", "our", or "the app") is committed to protecting and respecting your privacy. This policy outlines our handling of user data and Android device permissions.

---

### 1. No Data Collection or Transmission
Virtual Gamepad does **not** collect, record, store, transmit, or share any personal data, device identifiers, IP addresses, location data, analytics, or gameplay activity. All user preferences, joystick configurations, and overlay settings are saved exclusively on your local device via standard Android application storage.

---

### 2. Android Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`)
The app utilizes Android's `AccessibilityService` API exclusively for the following functionality:
* **Simulated Touch Input:** To perform synthetic drag and tap gestures on the game screen when you manipulate the on-screen virtual joystick.

#### Important Disclosures:
* **No Screen Monitoring:** The Accessibility Service is **never** used to inspect screen contents, read text, capture passwords, record keystrokes, view notifications, or monitor user behavior across other apps.
* **No Data Recording:** No information processed by the Accessibility Service is ever recorded, cached, or transmitted over any network.
* **Explicit User Control:** The service is only active when enabled by the user in Android Settings and operates only when the overlay is active.

---

### 3. Overlay Window Permission (`SYSTEM_ALERT_WINDOW`)
The app requests permission to display over other apps solely to draw the floating virtual joystick, edit controls, and floating toolbar on top of your selected games. This permission is not used to display external advertisements or capture screen content.

---

### 4. Internet and Network Connectivity
Virtual Gamepad operates completely offline and requires no internet access or network permissions to function.

---

### 5. Third-Party Services and Analytics
The application does not include any third-party advertising SDKs, tracking libraries, or analytics frameworks.

---

### 6. Contact Information
If you have questions, feedback, or concerns regarding this Privacy Policy, please open an issue on GitHub:
* GitHub Repository: [https://github.com/SagarMishra119/Virtual-GamePad](https://github.com/SagarMishra119/Virtual-GamePad)
