<div align="center">

# 🍩 justTeams

### *The Ultimate Team Management Experience*

[![Author](https://img.shields.io/badge/Author-kotori-lightgrey?style=for-the-badge&logo=github)](https://github.com/kotori)
[![API](https://img.shields.io/badge/API-1.21-brightgreen?style=for-the-badge&logo=java)](https://papermc.io/downloads)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge&logo=opensourceinitiative)](LICENSE)
[![Folia](https://img.shields.io/badge/Folia-Supported-green?style=for-the-badge&logo=serverless)](https://folia.papermc.io/)

*A powerful and modern Minecraft teams plugin, fully customizable for any Paper-based server*

---

</div>

## 🚀 **Quick Start**

```bash
# Download and install
wget https://github.com/kotori/justTeams/releases/latest/download/justTeams.jar
mv justTeams.jar plugins/

# Install dependencies
# - Vault (required for economy)
# - PlaceholderAPI (recommended)
```

---

## ✨ **Features**

| Feature | Description |
|---------|-------------|
| 🎯 **Team Management** | Role-based system with Owner/Co-Owner/Member |
| 🏦 **Team Banking** | Shared funds with Vault integration |
| 🗄️ **Shared Storage** | Team ender chest with cross-server sync |
| 🏠 **Teleportation** | Team homes and warps with security |
| 💬 **Team Chat** | Toggle team chat mode or use `/t <message>` |
| 🌐 **Cross-Server** | Real-time synchronization across your network |

---

## ⌨️ **Commands**

### **Basic Commands**
| Command | Description |
|---------|-------------|
| `/team create <name> <tag>` | Create a new team |
| `/team invite <player>` | Invite a player |
| `/team accept <team>` | Accept invitation |
| `/team leave` | Leave your team |
| `/team kick <player>` | Kick a player |
| `/team info` | Show team information |

### **Management Commands**
| Command | Description |
|---------|-------------|
| `/team promote <player>` | Promote to Co-Owner |
| `/team demote <player>` | Demote to Member |
| `/team transfer <player>` | Transfer ownership |
| `/team disband` | Disband your team |
| `/team pvp` | Toggle team PvP |
| `/team public` | Toggle public/private |

### **Home & Warp Commands**
| Command | Description |
|---------|-------------|
| `/team sethome` | Set team home |
| `/team delhome` | Delete team home |
| `/team home` | Teleport home |
| `/team setwarp <name>` | Create warp |
| `/team delwarp <name>` | Delete warp |
| `/team warp <name>` | Use warp |
| `/team warps` | List warps |

### **Economy Commands**
| Command | Description |
|---------|-------------|
| `/team bank` | Open bank GUI |
| `/team enderchest` | Open shared storage |

### **Chat Commands**
| Command | Description |
|---------|-------------|
| `/team chat` | Toggle team chat mode |
| `/t <message>` | Send team message |

---

## 🔐 **Permissions**

- `justteams.*` - All permissions
- `justteams.user` - Basic team commands
- `justteams.admin` - Admin commands

### **Bypass Permissions**
- `justteams.bypass.bank.withdraw`
- `justteams.bypass.enderchest.use`
- `justteams.bypass.home.cooldown`
- `justteams.bypass.warp.cooldown`

---

## 🧩 **PlaceholderAPI**

| Placeholder | Description |
|-------------|-------------|
| `%justteams_name%` | Team name |
| `%justteams_tag%` | Team tag |
| `%justteams_role%` | Player's role |
| `%justteams_member_count%` | Number of members |
| `%justteams_kills%` | Team kills |
| `%justteams_kdr%` | Kill/Death ratio |
| `%justteams_bank_balance%` | Team bank balance |

---

## ⚙️ **Configuration**

### **Main Settings**
```yaml
settings:
  max_team_size: 10
  default_pvp_status: true
  default_public_status: false

team_bank:
  enabled: true
  max_balance: 1000000.0

team_home:
  warmup_seconds: 5
  cooldown_seconds: 300
```

### **Storage Options**
- **H2**: File-based storage (single server)
- **MySQL**: Database storage (multi-server)

---

## 📚 **Wiki**

<div align="center">

### **Quick Guides**

| What do you want to do? | How to do it |
|------------------------|--------------|
| **Create your first team** | `/team create MyTeam [MT]` |
| **Invite a player** | `/team invite PlayerName` |
| **Accept an invitation** | `/team accept TeamName` |
| **Open team GUI** | `/team` or `/team gui` |
| **Set team home** | `/team sethome` |
| **Teleport to home** | `/team home` |
| **Access team bank** | `/team bank` |
| **Use team storage** | `/team enderchest` |
| **Toggle team chat** | `/team chat` |
| **Send team message** | `/t Hello team!` |

</div>

<div align="center">

### **Team Management**

| Action | Command | Permission Required |
|--------|---------|-------------------|
| **Promote player** | `/team promote PlayerName` | Owner/Co-Owner |
| **Demote player** | `/team demote PlayerName` | Owner/Co-Owner |
| **Kick player** | `/team kick PlayerName` | Owner/Co-Owner |
| **Transfer ownership** | `/team transfer PlayerName` | Owner only |
| **Disband team** | `/team disband` | Owner only |
| **Toggle PvP** | `/team pvp` | Owner/Co-Owner |
| **Make public/private** | `/team public` | Owner/Co-Owner |

</div>

<div align="center">

### **Team Features**

| Feature | How to Use | Description |
|---------|------------|-------------|
| **🏠 Team Home** | `/team sethome` → `/team home` | Set and teleport to team home |
| **🌐 Team Warps** | `/team setwarp name` → `/team warp name` | Create and use team warps |
| **🏦 Team Bank** | `/team bank` | Access shared team funds |
| **📦 Storage** | `/team enderchest` | Use shared team storage |
| **💬 Team Chat** | `/team chat` or `/t message` | Communicate with team |

</div>

<div align="center">

### **Permissions**

| Permission | What it does |
|------------|--------------|
| `justteams.user` | Basic team commands |
| `justteams.admin` | Admin commands |
| `justteams.bypass.bank.withdraw` | Bypass bank restrictions |
| `justteams.bypass.enderchest.use` | Bypass storage restrictions |
| `justteams.bypass.home.cooldown` | Bypass home cooldown |

</div>

<div align="center">

### **Configuration**

| File | What it controls |
|------|------------------|
| `config.yml` | Main plugin settings |
| `messages.yml` | All text and messages |
| `gui.yml` | GUI appearance |
| `commands.yml` | Command configuration |

</div>

<div align="center">

### **Common Issues**

| Problem | Solution |
|---------|----------|
| **"No permission"** | Check player permissions |
| **"Team not found"** | Make sure team exists |
| **"Player not in team"** | Join a team first |
| **"GUI not opening"** | Check permissions and team status |
| **"Database error"** | Check MySQL/H2 configuration |

</div>

---

## 🤝 **Support**

- **Issues**: [GitHub Issues](https://github.com/kotori/justTeams/issues)
- **Discord**: [Join Server](https://discord.gg/Am7D6Qz9)

---

<div align="center">

## 🎉 **Ready to Get Started?**

[![Download](https://img.shields.io/badge/Download-Latest-brightgreen?style=for-the-badge&logo=download)](https://builtbybit.com/resources/justteams.71401/)
[![Documentation](https://img.shields.io/badge/Documentation-Wiki-blue?style=for-the-badge&logo=book)](https://github.com/kotori/justTeams/wiki)
[![Support](https://img.shields.io/badge/Support-Discord-purple?style=for-the-badge&logo=discord)](https://discord.gg/Am7D6Qz9)

---

**🍩 justTeams** - *The ultimate team management solution for modern Minecraft servers*

*Built with ❤️ by [**kotori**](https://github.com/kotori)*

</div>
