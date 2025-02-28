# WebsiteCaptcha

WebsiteCaptcha is a Minecraft server plugin designed to enhance security by requiring players to verify themselves via a web-based CAPTCHA before accessing the main game world. This plugin integrates with a SQLite database for persistent verification data and offers extensive customization options to tailor the verification experience to your server’s needs.

## Disclaimer
WebsiteCaptcha is an experimental plugin and may contain numerous bugs and glitches. Developed as a proof-of-concept, it might exhibit instabilities, especially under high player loads or with certain server configurations. Features such as the embedded web server, SQLite database, and timeout mechanics could lead to unexpected behavior. Use this plugin at your own risk, and thoroughly test it in a controlled environment before deploying it on a live server. Contributions to fix bugs or enhance stability are highly encouraged!

## Plugin Information
WebsiteCaptcha protects your Minecraft server from unauthorized access by implementing a CAPTCHA verification system. Upon joining, unverified players are sent to a configurable "Limbo" world where they must complete a CAPTCHA challenge hosted on a local web server (default: `localhost:8080`). Once verified, players gain access to the main world. The plugin includes features like an XP bar countdown timer, configurable game modes, flight options, and visual effects like blindness to manage unverified players effectively.

### Key Features
- **CAPTCHA Verification**: Players complete a Google reCAPTCHA challenge via a web link.
- **Limbo World**: Unverified players are isolated in a separate world with restricted permissions.
- **XP Bar Timer**: Displays remaining verification time, decreasing smoothly until timeout.
- **Configurable Settings**: Customize timeout duration, game mode, flight, blindness, and inventory hiding via `config.yml`.
- **Kick Delay**: Displays a configurable message for 5 seconds before kicking on timeout.
- **Version Detection**: Logs player Minecraft versions (1.8 to 1.21.4) using ProtocolLib.
- **Persistent Storage**: Stores verification timestamps and states in SQLite (`verification.db`).

### How It Works
1. **Player Joins**: The server logs the player’s IP, name, and version, then checks their verification status.
2. **Verification Check**: If unverified or the grace period has expired, the player is sent to Limbo.
3. **CAPTCHA Prompt**: A clickable chat message provides a link to the CAPTCHA page.
4. **Timeout Mechanism**: An XP bar counts down; if time runs out, a kick message appears for 5 seconds before disconnection.
5. **Success**: Upon CAPTCHA completion, the player is teleported to the main world with restored settings.

## Installation
1. **Dependencies**:
   - **Paper/Bukkit/Spigot**: Compatible with versions 1.20 to 1.21.4.
   - **ProtocolLib**: Required for Limbo system and version detection (`getPlayerVersion`).
2. **Steps**:
   - Download the latest `WebsiteCaptcha.jar` from the [Releases](https://github.com/SalmanDev/WebsiteCaptcha/releases) page.
   - Place it in your server’s `plugins` folder.
   - Install ProtocolLib and ensure it’s loaded.
   - Start the server to generate `config.yml` in `plugins/WebsiteCaptcha/`.
   - Configure `config.yml` with your reCAPTCHA keys and preferences.

## Configuration
The plugin uses a config.yml file for customization. Below is the default configuration:
```yaml
# ==========================
# WebsiteCaptcha Plugin Configuration
# ==========================
# This configuration ensures players complete a CAPTCHA on a designated website
# before accessing the server. Players are temporarily placed in a virtual
# lobby and prompted to verify. Once verified, they can proceed to the main server.

# --------------------------
# Messages
# --------------------------
# Messages displayed to players during the verification process. Use '&' for color codes.
# Examples: &a (green), &c (red), &e (yellow), &b (blue). Use '\n' for line breaks.
messages:
  already_verified: "&aWelcome back! You are still verified."
  verification_prompt: "&eWelcome! Please verify yourself by clicking this link: "
  verification_link_text: "&b[Verify Here]"
  verification_success: "&aThank you for verifying! Enjoy the server."
  verification_failed: "&cVerification failed. Please try again later."
  verification_timeout_kick: "&cVerification failed. Please try again later.\n&4You appear to be a bot!"
  no_command_permission: "&cYou cannot use commands until you are verified."
  no_chat_permission: "&cYou cannot chat until you are verified."
  inventory_hidden: "&cYour inventory is hidden until you verify."
  inventory_restored: "&aYour inventory has been restored after verification."
  block_breaking: "&cYou cannot break blocks while unverified!"
  block_placing: "&cYou cannot place blocks while unverified!"
  item_dropping: "&cYou cannot drop items while unverified!"
  item_picking_up: "&cYou cannot pick up items while unverified!"
  inventory_clicking: "&cYou cannot interact with the inventory while unverified!"
  attack: "&cYou cannot attack while unverified!"
  receive_damage: "&cYou cannot receive damage while unverified!"
  reload_success: "§aPlugin configuration reloaded successfully!"
  no_permission: "§cYou do not have permission to use this command."
  clear_success: "§aVerification cleared for player: %player%"
  set_success: "§aVerification successfully set for player: %player%"
  player_not_found: "§cPlayer not found."

# --------------------------
# Behavior Options
# --------------------------
# Controls for player restrictions during the verification process.
options:
  # Apply a blindness effect to unverified players. If disabled, ensure any existing
  # blindness effects are removed to avoid issues.
  apply_blindness: true

  # Game mode for unverified players in Limbo (SURVIVAL, SPECTATOR, ADVENTURE, CREATIVE)
  game_mode: ADVENTURE  # Default: ADVENTURE

  # Time (in seconds) players have to verify before being kicked
  verification_timeout: 60

  # Duration (in milliseconds) a verification session remains valid before requiring re-verification
  verification_grace_period: 86400000  # 24 hours

  # Whether to send a title to unverified players in Limbo (true/false)
  SendTitle: false
  # Title text to display (supports color codes with &)
  title: "&aWelcome To Server"
  # Subtitle text to display (supports color codes with &)
  subtitle: "&aThanks for using my plugin"

  # Whether to send an action bar message to unverified players in Limbo (true/false)
  SendActionBar: false
  # Action bar text to display (supports color codes with &)
  actionBar: "&ePlease verify to join!"

  # Whether to send a chat message to unverified players in Limbo (true/false)
  SendChatMessage: false
  # Chat message text to display (supports color codes with &)
  chatMessage: "&eClick the link to verify!"

  # Prevent unverified players from moving. If false, they can move freely.
  prevent_unverified_player_movement: true

  # Prevent unverified players from using any commands.
  prevent_command_use: true

  # Prevent unverified players from chatting.
  prevent_chat: true

  # Hide unverified players' inventory. Restores inventory upon verification.
  hide_inventory: true

  # Restrictions on unverified players' block and item interactions.
  blockBreaking: false        # False = Block breaking is blocked
  blockPlacing: false         # False = Block placing is blocked
  itemDrop: false             # False = Item dropping is blocked
  itemPickup: false           # False = Item pickup is blocked
  inventoryClick: false       # False = Inventory clicking is blocked

  # Restrictions on damage-related actions for unverified players.
  damage:
    attack: false             # False = Players cannot attack others
    receive: false            # False = Players cannot receive damage

# --------------------------
# Web Server Configuration
# --------------------------
# Configuration for the verification web server. Ensure the port is open if hosting externally.
Web:
  # Host Address:
  # 1. This option must be filled with either:
  #    - The machine's IP address (e.g., "192.168.1.100") if hosting locally.
  #    - A domain name (e.g., "verify.yourserver.com") if hosting externally.
  # 2. Why it's important:
  #    - The host value is used to generate the verification link sent to players upon joining.
  #    - If left empty, the default value will be "localhost," which resolves to the machine's internal loopback IP (127.0.0.1).
  #    - Links with "localhost" will only work for the same machine where the server is running. Other players won't be able to access the verification website.
  # 3. Local Hosting:
  #    - Use your machine's local IP (e.g., "192.168.x.x") to allow players on the same network to access the verification link.
  #    - Example: "http://192.168.1.100:8080".
  # 4. External Hosting:
  #    - Use a domain name (e.g., "verify.yourserver.com") and configure DNS to point to your server.
  #    - Ensure the specified port (see below) is open and accessible through your firewall and router.
  # 5. If this value is not configured:
  #    - Players will receive a verification link with "localhost," which will not work for anyone other than the server's host machine.
  host: ""
  # Port Configuration:
  # 1. Specify the port number for the web server:
  #    - Default: 8080
  #    - You can use any available port (e.g., 9090, 2020, etc.).
  # 2. Why this is important:
  #    - The port must be open and accessible for the verification website to function.
  #    - Players will receive a link in the format: "http://<host>:<port>". Ensure the port is correct and accessible.
  # 3. Hosting Notes:
  #    - If you're hosting the server locally, ensure the port is not used by other services.
  #    - For external hosting, ensure the port is open in your firewall and router. Use command /webcaptcha portcheck <ip> <port> to verify that the port is open.
  # 4. Examples:
  #    - Local setup: port: 8080 (access via http://192.168.x.x:8080).
  #    - External setup: port: 9090 (access via http://verify.yourserver.com:9090).
  port: 8080

# --------------------------
# reCAPTCHA Configuration
# --------------------------
# Settings for Google reCAPTCHA (v2) integration on the verification website.
recaptcha:
  # Site key for identifying your website to the reCAPTCHA service.
  # Obtain this key from the Google reCAPTCHA admin console.
  site_key: ""

  # Secret key for secure server-side communication with the reCAPTCHA service.
  # Keep this key confidential.
  secret_key: ""

```

For detailed configuration options, refer to the `config.yml` file in the repository.

## Contributing
This plugin is open-source under the GNU General Public License v3.0. We welcome contributions from the community to improve and grow WebsiteCaptcha! You can:

- Report bugs or suggest features via [Issues](https://github.com/Noonenowhoiam/WebSiteCpatcha/issues).
- Fork the repository, make enhancements, and submit Pull Requests.
- Optimize performance, squash bugs, or add new features to enhance its reliability and utility.

Contributions must adhere to the GNU GPL v3.0, ensuring that any modifications remain open-source and freely available. Check out the repository, dive into the code, and help us make WebsiteCaptcha a robust security solution for Minecraft servers!

## License
WebsiteCaptcha is licensed under the GNU General Public License v3.0. This license grants you the freedom to use, study, share, and modify the software, provided any derivative works are distributed under the same terms. See the [LICENSE](https://github.com/Noonenowhoiam/WebSiteCpatcha/blob/main/LICENSE) file for the full text.

Key aspects of GPL v3.0:
- **Freedom**: Use, modify, and distribute the plugin freely.
- **Copyleft**: Any changes or derived works must remain open-source under GPL v3.0.
- **No Warranty**: The plugin is provided "as is" with no guarantees of fitness or performance.

## Credits
- **Author**: SalmanDev (GitHub: [Noonenowhoiam](https://github.com/Noonenowhoiam))
- **Dependencies**:
  - ProtocolLib by dmulloy2 and contributors.

## Contact or Support

-   For issues, queries, or suggestions, please use the project's [GitHub Issues](https://github.com/Noonenowhoiam/WebSiteCpatcha/issues).
-   For personal queries urgent matters or help you can contact me on Discord: `@apgamingboy`.
