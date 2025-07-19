
# 🍩 DonutTeams

A powerful and modern Minecraft **teams plugin**, built for **DonutSMP** and fully customizable for any **Paper-based server**.

<p align="center">
  <img src="https://img.shields.io/badge/Author-kotori-lightgrey?style=for-the-badge" alt="Author" />
  <img src="https://img.shields.io/badge/API-1.21-brightgreen?style=for-the-badge" alt="API Version" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License" />
</p>

---

## ✨ Features

- 👥 **Full Team Management** — Create, invite, kick, disband, and manage teams.
- 🏦 **Team Bank** — Let teams pool their money with Vault integration.
- 🗄️ **Team Ender Chest** — A shared, persistent inventory for every team.
- 🏆 **Stats & Leaderboards** — Track team kills, deaths, and KDR, and display top teams.
- 🎨 **Highly Customizable** — Change team size, name limits, colors, and all messages.
- 💾 **Flexible Storage** — Use local H2 or MySQL for multi-server setups.
- ⚙️ **Automatic Migration** — Update the plugin without resetting configs or database.
- 🏠 **Team Homes** — Set and teleport to a team home with cooldown/warmup.
- ⚔️ **PvP Toggle** — Enable/disable friendly fire per team.
- 💬 **Private Team Chat** — Toggle team-only chat or use `/teammsg`.
- 🖥️ **Full GUI Interface** — Manage everything from the bank to member permissions visually.
- 💎 **MiniMessage Support** — Stylish gradients, click/hover effects.
- 🧩 **Full PlaceholderAPI Support** — Display team info anywhere.

---

## 📦 Installation

1. Download the latest `DonutTeams.jar`.
2. Stop your Minecraft server.
3. Place the file in the `plugins/` folder.
4. _(Required for Bank)_ Install **[Vault](https://www.spigotmc.org/resources/vault.34315/)**.
5. _(Recommended)_ Install **[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)**.
6. Start the server to generate configuration files.
7. Edit `config.yml` and `messages.yml` as desired.
8. Reload the config with `/team reload`.

---

## ⚙️ Configuration

### `config.yml`

```yaml
# ----------------------------------------------------
#  DonutTeams Configuration
# ----------------------------------------------------
#  This file contains all the main settings for the plugin.
#  For message customization, please see messages.yml.
#
#  CONFIG VERSION - DO NOT CHANGE THIS
config-version: 3
# ----------------------------------------------------

# Storage settings
storage:
  type: "h2"
  mysql:
    enabled: false
    host: "localhost"
    port: 3306
    database: "donutsmp"
    username: "root"
    password: ""

# General team settings
settings:
  main_color: "#4C9DDE"
  accent_color: "#4C96D2"
  max_team_size: 10
  min_name_length: 3
  max_name_length: 16
  max_tag_length: 6
  max_description_length: 64
  default_pvp_status: true

# Team Home feature settings
team_home:
  warmup_seconds: 5
  cooldown_seconds: 300

# Team Bank feature settings
team_bank:
  max_balance: 1000000.0

# Team Ender Chest feature settings
team_enderchest:
  rows: 3

# Webhook settings
webhook:
  enabled: true
```

### `messages.yml` (Supports MiniMessage)

```yaml
prefix: "<b><gradient:#4C9DDE:#4C96D2>ᴛᴇᴀᴍs</gradient></b> <dark_gray>| <gray>"

team_created: "<green>You have successfully created the team <white><team></white>."
bank_deposit_success: "<green>You deposited <white><amount></white> into the team bank."
invite_received: |
  You have been invited to join <white><team></white>.
  <click:run_command:/team accept <team>><hover:show_text:'<green>Click to accept!'><green>[Accept]</hover></click> or <click:run_command:/team deny <team>><hover:show_text:'<red>Click to deny!'><red>[Deny]</hover></click>
```

---

## ⌨️ Commands & Permissions

### 🔧 Commands

| Command | Aliases | Description |
|--------|---------|-------------|
| `/team` | `t, clan, party` | Show team info or help. |
| `/team create <name> <tag>` | - | Create a new team. |
| `/team disband` | - | Disband your team (owner only). |
| `/team invite <player>` | - | Invite a player to your team. |
| `/team accept <team>` | - | Accept a team invitation. |
| `/team deny <team>` | - | Deny a team invitation. |
| `/team leave` | - | Leave your current team. |
| `/team kick <player>` | - | Kick a member (owner only). |
| `/team info [team]` | - | Show detailed team info. |
| `/team chat` | `c` | Toggle team-only chat. |
| `/team gui` | - | Open the team management GUI. |
| `/team sethome` | - | Set the team's home location. |
| `/team home` | - | Teleport to team home. |
| `/team settag <tag>` | - | Change your team tag. |
| `/team setdescription <desc>` | `setdesc` | Set the team description. |
| `/team transfer <player>` | - | Transfer ownership. |
| `/team pvp` | - | Toggle friendly fire. |
| `/team bank` | - | Open the team bank GUI. |
| `/team bank <deposit/withdraw> <amount>` | - | Quick bank actions. |
| `/team enderchest` | `ec` | Open shared ender chest. |
| `/team top` | `leaderboard` | View team leaderboard. |
| `/team reload` | - | Reload config (admin only). |
| `/teammsg <message>` | `tm, tmsg` | Send message to team chat. |

### 🔐 Permissions

| Permission | Description |
|------------|-------------|
| `donutteams.user` | Grants access to all player commands. |
| `donutteams.admin` | Grants access to all admin commands. |
| `donutteams.command.*` | Wildcard for all command permissions. |

**Recommended Setup:**

- Players: `donutteams.user`
- Admins: `donutteams.admin`

---

## 🧩 PlaceholderAPI

DonutTeams supports **PlaceholderAPI** out of the box.

| Placeholder | Description |
|------------|-------------|
| `%donutteams_name%` | Player's team name |
| `%donutteams_tag%` | Team's tag |
| `%donutteams_description%` | Team description |
| `%donutteams_owner%` | Team owner's name |
| `%donutteams_member_count%` | Current number of members |
| `%donutteams_max_members%` | Max team size |
| `%donutteams_members_online%` | Online team members |
| `%donutteams_role%` | Player's role (Owner or Member) |
| `%donutteams_kills%` | Total team kills |
| `%donutteams_deaths%` | Total team deaths |
| `%donutteams_kdr%` | Team K/D ratio |
| `%donutteams_bank_balance%` | Formatted team bank balance |

---

<p align="center">Made with ❤️ by <strong>kotori</strong></p>
