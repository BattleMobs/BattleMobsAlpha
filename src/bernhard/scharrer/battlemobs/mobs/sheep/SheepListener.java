package bernhard.scharrer.battlemobs.mobs.sheep;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import bernhard.scharrer.battlemobs.BattleMobs;
import bernhard.scharrer.battlemobs.mobs.MobListener;
import bernhard.scharrer.battlemobs.mobs.MobType;
import bernhard.scharrer.battlemobs.util.Cooldown;
import bernhard.scharrer.battlemobs.util.DamageHandler;
import bernhard.scharrer.battlemobs.util.Item;
import bernhard.scharrer.battlemobs.util.Locations;
import bernhard.scharrer.battlemobs.util.PlayerDeathListener;
import bernhard.scharrer.battlemobs.util.Task;
import bernhard.scharrer.battlemobs.util.Tier;
import de.robingrether.idisguise.disguise.Disguise;
import de.robingrether.idisguise.disguise.SheepDisguise;
import net.minecraft.server.v1_12_R1.EnumParticle;
import net.minecraft.server.v1_12_R1.PacketPlayOutWorldParticles;

public class SheepListener extends MobListener {

	private static final int WOOL_COUNT_SLOT = 3;
	private static final int WOOL_COUNT_PER_HIT = 1;
	private static final int WOOL_COUNT_DAMAGE_OFFSET = 5;
	private static final int WOOL_COUNT_PER_KILL = 5;
	private static final int WOOL_COUNT_MAX_BASE = 20;
	private static final double WOOL_COUNT_DAMAGE_MODIFIER = 0.15;
	private static final ItemStack WOOL_COUNTER = Item.createIngameItem("WOOL COUNTER", Material.WOOL, 0);
	private static final Material GRAZE_BLOCK_TYPE = Material.GRASS;
	private static final float GRAZE_TIMEOUT = 3;
	private static final PotionEffect JUMP_BOOST = new PotionEffect(PotionEffectType.JUMP, 80, 3);

	private static final List<Material> GRAZE_BANNED_BLOCKS = new ArrayList<>();
	private static final int FEEDING_TIME_HEAL = 4;
	private static final int FEEDING_TIME_REGENERATION = 2;
	private static final int FEEDING_TIME_COOLDOWN = 15;
	private static final float FEEDING_TIME_REGENERATION_TIMEOUT = 3;
	private static final double GRAZE_SLOW_RADIUS = 1.5;
	private static final PotionEffect GRAZE_SLOW = new PotionEffect(PotionEffectType.SLOW, 20, 2);

	private static List<Player> god_sheeps = new ArrayList<>();

	{
		GRAZE_BANNED_BLOCKS.add(Material.BARRIER);
		GRAZE_BANNED_BLOCKS.add(Material.DEAD_BUSH);
		GRAZE_BANNED_BLOCKS.add(Material.YELLOW_FLOWER);
		GRAZE_BANNED_BLOCKS.add(Material.SAPLING);
		GRAZE_BANNED_BLOCKS.add(Material.RED_MUSHROOM);
		GRAZE_BANNED_BLOCKS.add(Material.BROWN_MUSHROOM);
		GRAZE_BANNED_BLOCKS.add(Material.RED_ROSE);
		GRAZE_BANNED_BLOCKS.add(Material.WATER_LILY);
		GRAZE_BANNED_BLOCKS.add(Material.BED_BLOCK);
		GRAZE_BANNED_BLOCKS.add(Material.CHEST);
		GRAZE_BANNED_BLOCKS.add(Material.TRAPPED_CHEST);
		GRAZE_BANNED_BLOCKS.add(Material.ENDER_CHEST);
		GRAZE_BANNED_BLOCKS.add(Material.LONG_GRASS);
		GRAZE_BANNED_BLOCKS.add(Material.WEB);
		GRAZE_BANNED_BLOCKS.add(Material.GRASS);
		GRAZE_BANNED_BLOCKS.add(Material.LEVER);
		GRAZE_BANNED_BLOCKS.add(Material.WOOD_BUTTON);
		GRAZE_BANNED_BLOCKS.add(Material.STONE_BUTTON);
		GRAZE_BANNED_BLOCKS.add(Material.GRASS);
		GRAZE_BANNED_BLOCKS.add(Material.WALL_BANNER);
		GRAZE_BANNED_BLOCKS.add(Material.WALL_SIGN);
		GRAZE_BANNED_BLOCKS.add(Material.SIGN_POST);
	}
	
	public SheepListener() {
		super();
		new PlayerDeathListener() {
			public void onPlayerDeath(Player dead, Player killer) {
				if (dead != killer && SheepListener.this.valid(killer, MobType.SHEEP)) {
					
					int tier = SheepListener.this.getMobTier(killer);
					int max = getMaxWool(tier);
					int count;
					
					if (killer.getInventory().getItem(WOOL_COUNT_SLOT) != null) {
						count = killer.getInventory().getItem(WOOL_COUNT_SLOT).getAmount();
						count = count + WOOL_COUNT_PER_KILL > max ? max : count + WOOL_COUNT_PER_KILL;
						killer.getInventory().getItem(WOOL_COUNT_SLOT).setAmount(count);
					} else {
						killer.getInventory().setItem(WOOL_COUNT_SLOT, WOOL_COUNTER);
					}
				}
			}
		};
	}
	
	@EventHandler
	public void onHit(EntityDamageByEntityEvent e) {

		if (e.getDamager() instanceof Player) {
			Player p = (Player) e.getDamager();
			if (super.valid(p, MobType.SHEEP)) {
				if (p.getInventory().getItemInMainHand() != null) {

					ItemStack item = p.getInventory().getItemInMainHand();
					if (item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null) {
						if (item.getItemMeta().getDisplayName().contains(SheepItems.ABILITY_1_NAME)) {

							e.setCancelled(true);

							int tier = super.getMobTier(p);
							int count = 1;
							int max = getMaxWool(tier);

							if (p.getInventory().getItem(WOOL_COUNT_SLOT) != null) {
								count = p.getInventory().getItem(WOOL_COUNT_SLOT).getAmount();
								count = count + WOOL_COUNT_PER_HIT > max ? max : count + WOOL_COUNT_PER_HIT;
								p.getInventory().getItem(WOOL_COUNT_SLOT).setAmount(count);
							} else {
								p.getInventory().setItem(WOOL_COUNT_SLOT, WOOL_COUNTER);
							}

							p.playSound(p.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1, 1);

							p.updateInventory();

							if (e.getEntity() instanceof LivingEntity) {
								LivingEntity enemy = (LivingEntity) e.getEntity();
								
								DamageHandler.deal(enemy, p, WOOL_COUNT_DAMAGE_OFFSET+WOOL_COUNT_DAMAGE_MODIFIER * count);
								
							}

						}
					}
				}
			}
		}

	}

	@EventHandler
	public void onShearRightClick(PlayerInteractEvent e) {
		if (super.valid(e.getPlayer(), MobType.SHEEP) && e.getAction() == Action.RIGHT_CLICK_AIR
				|| e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player p = e.getPlayer();
			if (p.getInventory().getItemInMainHand() != null) {
				ItemStack item = p.getInventory().getItemInMainHand();
				if (item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null) {
					if (item.getItemMeta().getDisplayName().contains(SheepItems.ABILITY_1_NAME)) {

						int tier = super.getMobTier(p);
						ItemStack wool = p.getInventory().getItem(WOOL_COUNT_SLOT);
						if (wool != null) {
							int count = wool.getAmount();
							if (tier >= Tier.TIER_1_3) {
								if (count == getMaxWool(tier)) {
									if (!god_sheeps.contains(p)) {

										god_sheeps.add(p);

										new Task(0, 1) {
											private int n;

											@SuppressWarnings("deprecation")
											public void run() {
												if (SheepListener.this.valid(p, MobType.SHEEP)) {
													n++;
													n = n % DyeColor.values().length;
													DyeColor color = DyeColor.values()[n];

													Disguise d = BattleMobs.getAPI().getDisguise(p);

													if (d instanceof SheepDisguise) {

														SheepDisguise sheep = (SheepDisguise) d;
														sheep.setColor(color);
														BattleMobs.getAPI().disguise(p, sheep);
														p.addPotionEffect(JUMP_BOOST);

													}
												} else {
													p.removePotionEffect(PotionEffectType.JUMP);
													god_sheeps.remove(p);
													cancel();
												}
											}
										};

									}
								}
							}
						}

					}
				}
			}

		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {

		if (super.valid(e.getPlayer(), MobType.SHEEP) && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player p = e.getPlayer();
			if (p.getInventory().getItemInMainHand() != null) {
				ItemStack item = p.getInventory().getItemInMainHand();
				if (item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null) {
					if (item.getItemMeta().getDisplayName().contains(SheepItems.ABILITY_2_NAME)) {
						
						int tier = super.getMobTier(p);
						
						if (tier != Tier.UNDEFINED) {
							
							if (!GRAZE_BANNED_BLOCKS.contains(e.getClickedBlock().getType())) {
								List<Block> blocks = new ArrayList<>();
								
								blocks.add(e.getClickedBlock());
								blocks.add(e.getClickedBlock().getRelative(BlockFace.NORTH));
								blocks.add(e.getClickedBlock().getRelative(BlockFace.SOUTH));
								blocks.add(e.getClickedBlock().getRelative(BlockFace.EAST));
								blocks.add(e.getClickedBlock().getRelative(BlockFace.WEST));
								
								for (Block block : blocks) {
									if (!GRAZE_BANNED_BLOCKS.contains(block.getType())) {
										replaceBlock(GRAZE_BLOCK_TYPE, block, p);
									}
								}
								
								Block onTop = e.getClickedBlock().getRelative(BlockFace.UP);
								if (onTop.getType() == Material.AIR) {
									replaceBlock(getGrazeMaterial(tier), onTop, p);
									new SlowTracker(onTop.getLocation(), p);
								}
							}
							
						}

					}
				}
			}
		}

	}
	
	private class SlowTracker {
		
		private org.bukkit.entity.Item item;
		
		public SlowTracker(Location tracker, Player p) {
			
			item = p.getWorld().dropItem(tracker.add(0.5, 0.5, 0.5), Item.createItem("", "", Material.CAKE_BLOCK, 1, 0));
			item.setVelocity(new Vector(0, 0, 0));
			
			Task period = new Task(0,0.2f) {
				public void run() {
					if (item != null) {
						
						Location loc = item.getLocation().add(Vector.getRandom().multiply(0.5));
						PacketPlayOutWorldParticles particles = new PacketPlayOutWorldParticles(EnumParticle.REDSTONE, true, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(), 1, 0, 0, 0, 0, 0);
						
						for (Entity nearBy : item.getNearbyEntities(10, 10, 10)) {
							if (nearBy instanceof Player) {
								((CraftPlayer) nearBy).getHandle().playerConnection.sendPacket(particles);
							}
						}
						
						for (Entity nearBy : item.getNearbyEntities(GRAZE_SLOW_RADIUS, GRAZE_SLOW_RADIUS, GRAZE_SLOW_RADIUS)) {
							if (nearBy instanceof LivingEntity) {
								if (nearBy != p) {
									((LivingEntity) nearBy).addPotionEffect(GRAZE_SLOW);
								}
							}
						}
					}
				}
			};
			
			new Task(GRAZE_TIMEOUT) {
				public void run() {
					if (item != null) item.remove();
					period.cancel();
				}
			};
			
		}
		
	}
	
	@SuppressWarnings("deprecation")
	private void replaceBlock(Material material, Block block, Player p) {
		Material type = block.getType();
		byte data = block.getData();
		p.getWorld().getBlockAt(block.getLocation()).setType(material);

		new Task(GRAZE_TIMEOUT) {
			@Override
			public void run() {
				Locations.map_world.getBlockAt(block.getLocation()).setType(type);
				Locations.map_world.getBlockAt(block.getLocation()).setData(data);
			}
		};
	}

	@EventHandler
	public void onRightClick(PlayerInteractEvent e) {

		if (super.valid(e.getPlayer(), MobType.SHEEP) && e.getAction() == Action.RIGHT_CLICK_AIR
				|| e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player p = e.getPlayer();

			int tier = super.getMobTier(p);
			int heal = getHeal(tier);
			int cooldown = getHealCooldown(tier);

			if (p.getInventory().getItemInMainHand() != null) {
				ItemStack item = p.getInventory().getItemInMainHand();
				if (item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null) {
					if (item.getItemMeta().getDisplayName().contains(SheepItems.ABILITY_3_NAME)) {
						if (tier >= Tier.TIER_3_3) {
							
							Task heal_task = new Task(0, 0.5f) {
								public void run() {
									
									if (SheepListener.this.valid(p, MobType.SHEEP)) {
										if (p.getHealth() + FEEDING_TIME_REGENERATION >= p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()) {
											p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
										} else {
											p.setHealth(p.getHealth() + FEEDING_TIME_REGENERATION);
										}
									} else {
										cancel();
									}
									
								}
							};
							
							new Task(FEEDING_TIME_REGENERATION_TIMEOUT) {
								public void run() {
									heal_task.cancel();
								}
							};

							new Cooldown(p, 2, cooldown);
							if (p.getHealth() + heal >= p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()) {
								p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
							} else {
								p.setHealth(p.getHealth() + heal);
							}
						}
					}
				}
			}
		}
	}

	private int getMaxWool(int tier) {

		if (tier >= Tier.TIER_1_3)
			return WOOL_COUNT_MAX_BASE + 10;
		else if (tier >= Tier.TIER_1_2)
			return WOOL_COUNT_MAX_BASE + 5;
		else
			return WOOL_COUNT_MAX_BASE;

	}

	private int getHeal(int tier) {

		if (tier >= Tier.TIER_3_3)
			return FEEDING_TIME_HEAL + 6;
		else if (tier >= Tier.TIER_3_2)
			return FEEDING_TIME_HEAL + 4;
		else
			return FEEDING_TIME_HEAL;

	}

	private int getHealCooldown(int tier) {

		if (tier >= Tier.TIER_3_3)
			return FEEDING_TIME_COOLDOWN + 20;
		else if (tier >= Tier.TIER_3_2)
			return FEEDING_TIME_COOLDOWN + 10;
		else
			return FEEDING_TIME_HEAL;

	}
	
	private Material getGrazeMaterial(int tier) {

		if (tier >= Tier.TIER_2_3)
			return Material.RED_ROSE;
		else if (tier >= Tier.TIER_2_2)
			return Material.LONG_GRASS;
		else
			return Material.DEAD_BUSH;

	}

}
