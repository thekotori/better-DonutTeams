# 🍩 DonutTeams

> A powerful and modern Minecraft teams plugin, built for DonutSMP and fully customizable for any Paper-based server.

![Author](https://img.shields.io/badge/Author-kotori-lightgrey?style=for-the-badge)
![API](https://img.shields.io/badge/API-1.21-brightgreen?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

---

## ✨ Features

- **👥 Full Team Management** — Create, invite, kick, disband, and manage teams.
- **🎨 Highly Customizable** — Change team size, name limits, messages, colors, and more.
- **💾 Flexible Storage** — Use local **H2** or **MySQL** for multi-server setups.
- **🏠 Team Homes** — Set and teleport to a team home with cooldown/warmup.
- **⚔️ PvP Toggle** — Enable/disable friendly fire per team.
- **💬 Private Team Chat** — Toggle team-only chat or use `/teammsg`.
- **🖥️ GUI Interface** — Use `/team gui` to manage teams visually.
- **📊 Member Sorting** — Sort members by join date, name, or online status.
- **💎 MiniMessage Support** — Stylish gradients, click/hover effects.
- **🧩 PlaceholderAPI Ready** — Integration planned in future versions.

---

## 📦 Installation

1. Download the latest `DonutTeams.jar`.
2. Stop your Minecraft server.
3. Place the file in the `plugins/` folder.
4. *(Optional)* Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).
5. Start the server to generate configuration files.
6. Edit `config.yml` and `messages.yml` as desired.
7. Reload config with `/team reload`.

---

## ⚙️ Configuration

### `config.yml`

```yaml
# ----------------------------------------------------
#  DonutTeams Configuration
# ----------------------------------------------------
#  This file contains all the main settings for the plugin.
#  For message customization, please see messages.yml.
# ----------------------------------------------------

# Storage settings
# Configure how the plugin stores its data.
storage:
  # Can be "mysql" for a central database or "h2" for local file-based storage.
  # H2 is recommended for smaller servers as it requires no setup.
  type: "h2"
  
  # MySQL database settings.
  # Only used if type is "mysql" and enabled is "true".
  mysql:
    enabled: false
    host: "localhost"
    port: 3306
    database: "donutsmp"
    username: "root"
    password: ""

# General team settings
settings:
  # The main color used in GUIs and messages.
  main_color: "#95FD95"
  # The accent color used for highlights.
  accent_color: "#FFFFFF"
  # Maximum number of players allowed in a single team.
  max_team_size: 10
  # Minimum character length for a team name.
  min_name_length: 3
  # Maximum character length for a team name.
  max_name_length: 16
  # Maximum character length for a team tag.
  max_tag_length: 6
  # Default PvP status for newly created teams. If true, members can hurt each other.
  default_pvp_status: true

# Team Home feature settings
team_home:
  # Time in seconds a player must stand still before being teleported.
  # Set to 0 to disable the warmup.
  warmup_seconds: 5
  # Cooldown in seconds before a player can use /team home again.
  cooldown_seconds: 300 # 5 minutes

# Webhook settings
# This feature sends anonymous startup statistics to the developer to help improve the plugin.
# IP and Port is being sent to us. You can disable it at any time.
webhook:
  enabled: true
```

---

## 📝 Messages (`messages.yml`)

Supports full [MiniMessage](https://docs.advntr.dev/minimessage/format.html) syntax.

```yaml
prefix: "<gradient:#95FD95:#FFFFFF><bold>ᴛᴇᴀᴍs</bold></gradient> <dark_gray>| <gray>"

team_created: "<green>You have successfully created the team <white><team></white>."
name_too_short: "<red>The team name must be at least <white><min_length></white> characters long."

invite_received: |
  You have been invited to join <white><team></white>.
  <click:run_command:/team accept <team>><hover:show_text:'<green>Click to accept!'><green>[Accept]</hover></click>
  or
  <click:run_command:/team deny <team>><hover:show_text:'<red>Click to deny!'><red>[Deny]</hover></click>
```

---

## ⌨️ Commands & Permissions

### 🔧 Commands

| Command                        | Aliases              | Permission                      | Description                             |
|-------------------------------|----------------------|----------------------------------|-----------------------------------------|
| `/team`                       | `t`, `clan`, `party` | `donutteams.command.info`       | Show team info                          |
| `/team help`                  | -                    | `donutteams.command.help`       | Show help menu                          |
| `/team create <name> <tag>`   | -                    | `donutteams.command.create`     | Create a new team                       |
| `/team disband`               | -                    | `donutteams.command.disband`    | Disband team (owner only)              |
| `/team invite <player>`       | -                    | `donutteams.command.invite`     | Invite a player                         |
| `/team accept <team>`         | -                    | `donutteams.command.accept`     | Accept an invitation                    |
| `/team deny <team>`           | -                    | `donutteams.command.deny`       | Deny an invitation                      |
| `/team leave`                 | -                    | `donutteams.command.leave`      | Leave current team                      |
| `/team kick <player>`         | -                    | `donutteams.command.kick`       | Kick a member (owner only)             |
| `/team info [team]`           | -                    | `donutteams.command.info`       | Show team info                          |
| `/team chat`                  | `c`                  | `donutteams.command.chat`       | Toggle team chat                        |
| `/team gui`                   | -                    | `donutteams.command.gui`        | Open team GUI                           |
| `/team sethome`               | -                    | `donutteams.command.sethome`    | Set team home (owner only)             |
| `/team home`                  | -                    | `donutteams.command.home`       | Teleport to team home                   |
| `/team settag <tag>`          | -                    | `donutteams.command.settag`     | Set team tag (owner only)              |
| `/team transfer <player>`     | -                    | `donutteams.command.transfer`   | Transfer ownership                      |
| `/team pvp`                   | -                    | `donutteams.command.pvp`        | Toggle PvP (owner only)                |
| `/team reload`                | -                    | `donutteams.admin.reload`       | Reload plugin config                    |
| `/teammsg <message>`          | `tm`, `tmsg`         | `donutteams.command.teammsg`    | Send one message to team chat          |

---

### 🔐 Permissions

| Permission                   | Description                          |
|-----------------------------|--------------------------------------|
| `donutteams.user`           | Group node for all player commands   |
| `donutteams.admin`          | Full admin access (e.g. reload)      |
| `donutteams.command.*`      | Wildcard for all user commands       |

📌 **Recommended setup:**
- Players: `donutteams.user`
- Admins: `donutteams.admin`

---

## 🧩 PlaceholderAPI (Planned)

Support for PlaceholderAPI is coming soon!

| Placeholder                  | Description                                |
|-----------------------------|--------------------------------------------|
| `%donutteams_name%`         | Player's team name                         |
| `%donutteams_tag%`          | Player's team tag                          |
| `%donutteams_owner%`        | Team owner                                 |
| `%donutteams_member_count%` | Number of members in the team              |
| `%donutteams_max_members%`  | Maximum allowed members                    |
| `%donutteams_members_online%` | Online team members                     |
| `%donutteams_role%`         | Player's role (e.g., Owner, Member)        |

---

<p align="center">Made with ❤️ by kotori</p>
