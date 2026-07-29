# CoreEssentials

A Paper plugin for Minecraft 26.2 (the current newest Paper/Minecraft version)
providing: `/tpr`, `/home`, `/team`, `/rtp`, `/spawn` + `/setspawn`, and a
real, registry-backed **Vein Miner** enchantment.

## Requirements

- **JDK 25** (Paper 26.x requires it - `java -version` to check)
- **Maven 3.9+**
- A Paper **26.2** server to run it on

## Building

```
mvn clean package
```

The output jar will be at `target/coreessentials-1.0.0.jar`. Drop it into
your server's `plugins/` folder and restart the server.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/tpr <player>` | Send a teleport request. Target gets clickable Accept/Decline in chat. | `coreessentials.tpr` (default: everyone) |
| `/home` | List your homes | `coreessentials.home` |
| `/home <name>` | Teleport to a home | `coreessentials.home` |
| `/home set <name>` | Set a home (max 3, configurable) | `coreessentials.home` |
| `/home del <name>` | Delete a home | `coreessentials.home` |
| `/team create <name>` | Create a team | `coreessentials.team` |
| `/team invite <player>` | Invite to your team (clickable Accept/Decline) | `coreessentials.team` |
| `/team chat` | Toggle team-only chat mode (everything you type goes only to your team until you toggle it off again) | `coreessentials.team` |
| `/team chat <message>` | Send a single message to your team without toggling the mode | `coreessentials.team` |
| `/team leave` / `disband` / `kick <player>` / `list` | Team management | `coreessentials.team` |
| `/rtp` | Teleport to a random safe location | `coreessentials.rtp` |
| `/spawn` | Teleport to spawn | none - always available to any player |
| `/setspawn` | Set the spawn to your location | **op only** |

## Vein Miner

A real, data-driven enchantment (not a fake lore trick) registered through
Paper's registry API. It works on **both pickaxes and axes**:
- **Pickaxe** + Vein Miner → chain-breaks connected **ore** blocks
- **Axe** + Vein Miner → chain-breaks connected **log** blocks (trees)

It shows up:
- At the **enchanting table** (weighted like a rare enchant)
- On the **anvil** (combine an enchanted book with a pickaxe)
- From **librarian villagers** (random chance per trade restock)

Breaking one ore/log block with a Vein-Miner tool chain-breaks connected
blocks of that same type (ore for pickaxes, logs for axes), up to
`blocks-per-level * enchant level`
(capped by `max-blocks`), damaging the tool once per extra block broken.
Tune this in `config.yml`.

## Team chat tag

Each team is assigned a random color when it's created (stored in
`teams.yml`, so it doesn't change on restart). Every team member's normal
chat messages show `[TeamName] PlayerName: message` with that color on the
tag. `/team chat` toggles a private team-only channel.

## Notes / things to double-check on your end

- I could not compile this in my own sandbox (no network access to
  `repo.papermc.io` from here), so please run `mvn package` and send me
  any compiler errors if it doesn't build clean - I'll fix immediately.
- The registry/enchantment API (`RegistryEvents.ENCHANTMENT`, Brigadier
  commands, `paper-plugin.yml` bootstrapper) is all confirmed against the
  current PaperMC docs as of this build, but it's a newer part of the API
  and could shift slightly between Paper builds.
- Data is stored in flat YAML files (`homes.yml`, `teams.yml`, `spawn.yml`)
  in the plugin's data folder - fine for a small/medium server, but let me
  know if you'd rather have SQLite for a bigger one.
