- Define all gg.rsmod.game.message.Messages to avoid byte skipping in the packet decoding net code
- Implement RSA for game packets
- Add support for npc collision flags since npcs shouldn't be able to walk on top of one another
- Implement 'params' for NpcCombatDef to store information such as species
- Instance map support
- Redo SimplePathFindingStrategy as current one is really bad and does not even support diagonal movement
- Implement position multipliers for GPI decoding