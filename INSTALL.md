# PrintCord Verifone Serial Connector - Installation & Setup Guide

This guide explains how to install, configure, and run the **Verifone Serial Printer Connector** for PrintCord across **Windows**, **macOS**, and **Linux**.

---

## 1. Prerequisites

Before installing the connector, ensure you have:

1. **Java Runtime Environment (JRE/JDK 21 or higher)**
   - Verify installation:
     ```bash
     java -version
     ```
2. **A physical Verifone serial receipt printer (or compatible ESC/POS serial printer)**
   - Connected via RS-232 serial cable or USB-to-Serial adapter (FTDI / Prolific / CH340).
3. **PrintCord Discord Bot Access**
   - Access to a Discord server with the PrintCord bot installed and permissions to run the `/register` command.

---

## 2. Installation & Building

### Option A: Building from Source with Gradle

1. Clone or download the repository:
   ```bash
   git clone <repo-url>
   cd verifone-serial-connector
   ```

2. Build the application:
   - **Linux / macOS:**
     ```bash
     ./gradlew build
     ```
   - **Windows (PowerShell / Command Prompt):**
     ```powershell
     .\gradlew.bat build
     ```

3. The compiled executable archive and distribution will be located in `build/distributions/` or can be run directly using Gradle or Java.

---

## 3. Configuration & Setup

The connector requires two setup phases:
1. **Serial & Printer Hardware Configuration** (COM port, BAUD rate, column width).
2. **Discord Bot Registration** (linking the connector with your Discord server using a registration code).

### Step 1: Run the Interactive Setup Wizard

Run the application with the `--setup` flag:

- **Via Gradle:**
  - Linux / macOS: `./gradlew run --args="--setup"`
  - Windows: `.\gradlew.bat run --args="--setup"`

- **Via JAR / Distribution:**
  ```bash
  java -jar verifone-serial-connector.jar --setup
  ```

During setup, you will be prompted for:
1. **Serial Port / Device**:
   - The wizard detects and displays available serial ports.
   - Enter the port index (e.g. `1`), or type the port name directly (e.g. `COM3` on Windows, `/dev/ttyUSB0` on Linux, `/dev/tty.usbserial` on macOS).
2. **BAUD Rate**:
   - Default is `9600`. Enter your printer's BAUD rate (e.g., `9600`, `19200`, `38400`, `115200`).
3. **Character Column Width**:
   - Number of characters per line (e.g., `24` for standard Verifone 24-column roll, `32`, `40`, `42`, or `48` for wider thermal paper).
4. **Feed Lines**:
   - Number of blank lines fed after printing a receipt (default is `4`).
5. **Printer Name**:
   - A friendly name displayed in Discord (e.g., `Front Desk Verifone`).

---

### Step 2: Register with the Discord Bot

1. Open Discord and execute the `/register` slash command with PrintCord.
2. The bot will return an 8-character registration code (format `XXXX-XXXX`, valid for 15 minutes).
3. If running `--setup` or starting the connector for the first time without registration, the application will prompt:
   ```text
   Enter registration code: 1234-5678
   ```
4. Enter your code. The connector will register with the PrintCord API and securely store your authentication token in the configuration file.

---

## 4. Configuration Storage Paths

Configuration is stored in a JSON file (`verifone-connector.json`) located in your operating system's standard application data directory:

- **Windows**:
  `%APPDATA%\PrintCord\verifone-connector.json`  
  *(typically `C:\Users\<Username>\AppData\Roaming\PrintCord\verifone-connector.json`)*
- **macOS**:
  `~/Library/Application Support/PrintCord/verifone-connector.json`
- **Linux / BSD**:
  `~/.config/printcord/verifone-connector.json`  
  *(or `$XDG_CONFIG_HOME/printcord/verifone-connector.json`)*

### Example Configuration File (`verifone-connector.json`)

```json
{
  "apiUrl": "http://printcord.canyonwolf.net/api/connectors",
  "apiToken": "your-secret-api-token",
  "connectorId": "your-connector-uuid",
  "printerName": "Verifone Serial Printer",
  "portName": "COM3",
  "baudRate": 9600,
  "dataBits": 8,
  "stopBits": 1,
  "parity": "None",
  "printWidth": 24,
  "lineFeedLines": 4,
  "pollIntervalSeconds": 5
}
```

You can manually edit this file or specify a custom configuration path using `--config <path>`.

---

## 5. Running the Connector

### Interactive Mode

Starts the connector and provides an interactive command console (`status`, `poll`, `quit`):

- **Linux / macOS:**
  ```bash
  ./gradlew run
  # or
  java -jar verifone-serial-connector.jar
  ```
- **Windows:**
  ```powershell
  .\gradlew.bat run
  # or
  java -jar verifone-serial-connector.jar
  ```

### Background / Daemon Mode

Runs continuously in the background without interactive console prompts. Ideal for services and background tasks:

- **Linux / macOS:**
  ```bash
  java -jar verifone-serial-connector.jar --background
  ```
- **Windows:**
  ```powershell
  java -jar verifone-serial-connector.jar --background
  ```

---

## 6. Running as a System Service

To keep the connector running continuously on system startup:

### Linux (systemd)

1. Create a systemd service file `/etc/systemd/system/printcord-connector.service`:
   ```ini
   [Unit]
   Description=PrintCord Verifone Serial Connector
   After=network.target

   [Service]
   Type=simple
   User=your-username
   ExecStart=/usr/bin/java -jar /opt/printcord/verifone-serial-connector.jar --background
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```
2. Enable and start the service:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable printcord-connector
   sudo systemctl start printcord-connector
   ```

### Windows (Task Scheduler or NSSM)

- **Using NSSM (Non-Sucking Service Manager):**
  ```powershell
  nssm install PrintCordConnector "C:\Program Files\Java\jdk-21\bin\java.exe" "-jar C:\path\to\verifone-serial-connector.jar --background"
  nssm start PrintCordConnector
  ```

---

## 7. Command-Line Reference

| Option | Alias | Description |
|---|---|---|
| `--setup` | `--configure` | Launches the interactive configuration wizard for COM port, BAUD rate, width, feed lines, and bot registration |
| `--url <url>` | | Sets / overrides the PrintCord API base URL |
| `--feed-lines <count>` | | Overrides the number of trailing paper feed lines after print jobs |
| `--background` | `-d`, `--daemon` | Runs the connector as a non-interactive background daemon |
| `--config <path>` | | Uses a custom configuration file path instead of the system default |
| `--deregister` | | Deregisters the connector from the PrintCord server and clears local credentials |
| `--help` | `-h` | Shows the help message with available flags |

---

## 8. Troubleshooting & OS-Specific Notes

### Serial Port Permissions on Linux
If you encounter permission denied errors opening `/dev/ttyUSB0` or `/dev/ttyS0` on Linux, ensure your user is added to the `dialout` or `uucp` group:
```bash
sudo usermod -aG dialout $USER
# Log out and log back in for changes to take effect
```

### Serial Port Names on macOS
On macOS, serial devices typically appear under `/dev/tty.usbserial-*` or `/dev/cu.usbserial-*`. Use the `--setup` wizard to detect and select the connected USB-to-serial device.

### 401 Unauthorized / Deregistration
If the connector receives a 401 response from the server, it indicates the connector was deregistered from Discord via `/deregister`. To reconnect, run the application and enter a new registration code from Discord.
