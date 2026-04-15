# AutoMiner
Autominer is a Paper 1.26.2 plugin that allows players to create signs using the essentials sign format that will automatically break the block in front of them, made to use on my Minecraft Network
in conjunction with the Iridium SkyBlock mod and its changes to cobblestone generators. The miners have tiers corresponding to the tiers of pickaxe (barring Netherite),
different tiers have both different mining speeds as well as different blocks they can break. You can limit players via permission to different numbers of auto miners as
well as different tiers of miner.

## Setup
To create an auto miner sign place a sign on any surface.

Line 1 of the sign must be: [Mine]

Line 2 of the sign must be the pickaxe tier from the list below with nothing else.

The block directly in front of the sign is what will be targetted for automatic mining.

## Mining Tiers
Each tier can mine the block and deepslate versions of any ores, and in the case of gold the nether gold ore. Each level can mine the block of the previous level (with
the exception of gold which can mine the same as the wooden tier as with Minecraft pickaxes).

| Tier | Can Mine | Mining Speed |
| ---- | -------- | ------------ |
| Wood | Cobblestone, Stone, Deepslate, Netherrack, Coal Ore, Glowstone, Nether Quartz Ore | 1 |
| Stone | Copper Ore, Iron Ore, Lapis Ore, Nether Bricks, Sea Lantern | 2 |
| Copper | Same as stone | 3 |
| Iron | Gold Ore, Redstone Ore, Emerald Ore, Diamond Ore | 4 |
| Gold | Same as wood | 5 |
| Diamond | Same as iron | 6 |

## Config
| Option | Values | Description |
| ------ | ------ | ----------- |
| base-progress | integer | The base mining speed to be used when calculating mining speed per tier. Lower = Quicker, Higher = Slower. |
| will-break-any | boolean | Whether the tier of auto miner changes which blocks can be broken with it. |

### Default Config
```
mining:
  base-progress: 40.0
  will-break-any: false
```

## Permissions
All tier permissions inherit the previous level of permission, eg if you assign autominer.type.stone they will also inherit autominer.type.wood.

| Permission | Description | Default |
| ---------- | ----------- | ------- |
| autominer.type.wood | Gives permission to place wood auto miner. | true |
| autominer.type.{type} | Gives permission for players to place respective tier of auto miner. | false |
| autominer.type.any | Gives permission for players to place any tier of auto miner. | false |
| autominer.place | Required to be able to place any kind of auto miner. | true |
| autominer.limit.1 | Limit player to placing 1 auto miner | true |
| autominer.limit.{1-5} | Limit player to being able to place {x} number of auto miners. | false |
| autominer.limit.unlimited | Allow player to place unlimited number of auto miners. | false |
| autominer.admin | Allow access to /autominer reload for configuration reload and bypass miner restrictions for type and limit. | false |
