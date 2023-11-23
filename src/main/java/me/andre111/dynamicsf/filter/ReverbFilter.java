/*
 * Copyright (c) 2023 Andre Schweiger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.andre111.dynamicsf.filter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.lwjgl.openal.EXTEfx;

import me.andre111.dynamicsf.config.Config;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;

public class ReverbFilter {
	private static int id = -1;
	private static int slot = -1;

	private static boolean enabled = false;
	private static int tickCount = 0;
	private static float prevDecayFactor = 0.0f;
	private static float prevRoomFactor = 0.0f;
	private static float prevSkyFactor = 0.0f;

	private static float density = 0.2f;
	private static float diffusion = 0.6f;
	private static float gain = 0.15f;
	private static float gainHF = 0.8f;
	private static float decayTime = 0.1f;
	private static float decayHFRatio = 0.7f;
	private static float reflectionsGain = 0.0f;
	private static float reflectionsDelay = 0.0f;
	private static float lateReverbGain = 0.0f;
	private static float lateReverbDelay = 0.0f;
	private static float airAbsorptionGainHF = 0.99f;
	private static int decayHFLimit = 1;

	public static void reinit() {
		if(id > 0) EXTEfx.alDeleteEffects(id);
		if(slot > 0) EXTEfx.alDeleteAuxiliaryEffectSlots(slot);
		
		id = EXTEfx.alGenEffects();
		slot = EXTEfx.alGenAuxiliaryEffectSlots();
	}
	
	public static void updateGlobal(MinecraftClient client) {
		if(client.world != null && client.player != null && client.isRunning()) {
			update(client);
		} else {
			reset();
		}
	}

	public static boolean updateSoundInstance(SoundInstance soundInstance) {
		if(!enabled) return false;
		if(reflectionsDelay <= 0 && lateReverbDelay <= 0) return false;
		if(id == -1) reinit();
		if(id <= 0 || slot <= 0) return false;
		return true;
	}

	public static int getSlot() {
		return slot;
	}

	private static void reset() {
		enabled = false;
		density = Config.getData().reverbFilter.density;
		diffusion = Config.getData().reverbFilter.diffusion;
		gain = Config.getData().reverbFilter.gain;
		gainHF = Config.getData().reverbFilter.gainHF;
		decayTime = Config.getData().reverbFilter.minDecayTime;
		decayHFRatio = Config.getData().reverbFilter.decayHFRatio;
		reflectionsGain = 0;
		reflectionsDelay = 0;
		lateReverbGain = 0;
		lateReverbDelay = 0;
		airAbsorptionGainHF = Config.getData().reverbFilter.airAbsorptionGainHF;
		
		density = MathHelper.clamp(density, EXTEfx.AL_REVERB_MIN_DENSITY, EXTEfx.AL_REVERB_MAX_DENSITY);
		diffusion = MathHelper.clamp(diffusion, EXTEfx.AL_REVERB_MIN_DIFFUSION, EXTEfx.AL_REVERB_MAX_DIFFUSION);
		gain = MathHelper.clamp(gain, EXTEfx.AL_REVERB_MIN_GAIN, EXTEfx.AL_REVERB_MAX_GAIN);
		gainHF = MathHelper.clamp(gainHF, EXTEfx.AL_REVERB_MIN_GAINHF, EXTEfx.AL_REVERB_MAX_GAINHF);
		decayHFRatio = MathHelper.clamp(decayHFRatio, EXTEfx.AL_REVERB_MIN_DECAY_HFRATIO, EXTEfx.AL_REVERB_MAX_DECAY_HFRATIO);
		airAbsorptionGainHF = MathHelper.clamp(gainHF, EXTEfx.AL_REVERB_MIN_AIR_ABSORPTION_GAINHF, EXTEfx.AL_REVERB_MAX_AIR_ABSORPTION_GAINHF);
		decayHFLimit = MathHelper.clamp(decayHFLimit, EXTEfx.AL_REVERB_MIN_DECAY_HFLIMIT, EXTEfx.AL_REVERB_MAX_DECAY_HFLIMIT);
	}

	private static void update(MinecraftClient client) {
		enabled = Config.getData().reverbFilter.enabled;
		int maxBlocks = Config.getData().reverbFilter.maxBlocks;
		boolean checkSky = Config.getData().reverbFilter.checkSky;
		float reverbPercent = Config.getData().reverbFilter.reverbPercent;
		float minDecayTime = Config.getData().reverbFilter.minDecayTime;
		float reflectionGainBase = Config.getData().reverbFilter.reflectionsGainBase;
		float reflectionGainMultiplier = Config.getData().reverbFilter.reflectionsGainMultiplier;
		float reflectionDelayMultiplier = Config.getData().reverbFilter.reflectionsDelayMultiplier;
		float lateReverbGainBase = Config.getData().reverbFilter.lateReverbGainBase;
		float lateReverbGainMultiplier = Config.getData().reverbFilter.lateReverbGainMultiplier;
		float lateReverbDelayMultiplier = Config.getData().reverbFilter.lateReverbDelayMultiplier;

		// get base reverb
		Identifier dimension = client.world.getRegistryKey().getValue();
		float baseReverb = Config.getData().reverbFilter.getDimensionBaseReverb(dimension);

		if(enabled && tickCount++ == 20) {
			tickCount = 0;
			
			if(id == -1) reinit();

			// scan surroundings
			BlockPos playerPos = new BlockPos(
					MathHelper.floor(client.player.getPos().getX()), 
					MathHelper.floor(client.player.getPos().getY() + client.player.getEyeHeight(client.player.getPose())), 
					MathHelper.floor(client.player.getPos().getZ())
					);

			// sample random blocks in surroundings
			Random random = new Random();
			Set<BlockPos> visited = new TreeSet<>();
			List<Identifier> blocksFound = new ArrayList<>();
			List<BlockPos> toVisit = new LinkedList<>();
			toVisit.add(playerPos);
			for (int i = 0; i < maxBlocks && !toVisit.isEmpty(); ++i) {
				BlockPos current = toVisit.remove(random.nextInt(toVisit.size()));
				visited.add(current);
				for(Direction direction : Direction.values()) {
					BlockPos pos = current.offset(direction);
					BlockState blockState = client.world.getBlockState(pos);
					Identifier blockID = Registries.BLOCK.getId(blockState.getBlock());
					if (!blockState.isSolidBlock(client.world, pos)) {
						if (!visited.contains(pos) && !toVisit.contains(pos)) {
							toVisit.add(pos);
						}
						if (!blockState.isAir() && blockState.getFluidState().isEmpty()) {
							blocksFound.add(blockID);
						}
					} else {
						blocksFound.add(blockID);
					}
				}
			}

			// calculate decay factor
			double highReverb = 0.0;
			double midReverb = 0.0;
			double lowReverb = 0.0;
			for (Identifier blockID : blocksFound) {
				if(Config.getData().reverbFilter.lowReverbBlocks.contains(blockID)) {
					lowReverb += 1.0;
				} else if(Config.getData().reverbFilter.highReverbBlocks.contains(blockID)) {
					highReverb += 1.0;
				} else {
					midReverb += 1.0;
				}
			}
			float decayFactor = baseReverb;
			if (highReverb + midReverb + lowReverb > 0.0) {
				decayFactor += (highReverb - lowReverb) / (highReverb + midReverb + lowReverb);
			}
			decayFactor = Math.max(0, Math.min(decayFactor, 1));

			// calculate room factor
			int roomSize = visited.size();
			float roomFactor = roomSize / (float) maxBlocks;

			// calculate sky factor
			float skyFactor = 0;
			if(checkSky && roomSize == maxBlocks) {
				if(hasSkyAbove(client.world, playerPos)) skyFactor += 1;
				Direction[] directions = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
				for(Direction direction : directions) {
					if(hasSkyAbove(client.world, playerPos.offset(direction, random.nextInt(5) + 5))) skyFactor += 1;
					if(hasSkyAbove(client.world, playerPos.offset(direction, random.nextInt(5) + 5).offset(Direction.UP, 5))) skyFactor += 1;
				}
			}
			skyFactor = 1.0f - skyFactor / 9.0f;
			skyFactor *= skyFactor;
			
			//DynamicSoundFilters.getLogger().info(highReverb + " " + midReverb + " " + lowReverb + " -> " + decayFactor + " " + roomFactor + " " + skyFactor);

			// interpolate values
			decayFactor = (decayFactor + prevDecayFactor) / 2.0f;
			roomFactor = (roomFactor + prevRoomFactor) / 2.0f;
			skyFactor = (skyFactor + prevSkyFactor) / 2.0f;
			prevDecayFactor = decayFactor;
			prevRoomFactor = roomFactor;
			prevSkyFactor = skyFactor;

			// update values
			decayTime = reverbPercent * 6.0f * decayFactor * roomFactor * skyFactor;
			if (decayTime < minDecayTime) {
				decayTime = minDecayTime;
			}
			reflectionsGain = reverbPercent * (reflectionGainBase + reflectionGainMultiplier * roomFactor);
			reflectionsDelay = reflectionDelayMultiplier * roomFactor;
			lateReverbGain = reverbPercent * (lateReverbGainBase + lateReverbGainMultiplier * roomFactor);
			lateReverbDelay = lateReverbDelayMultiplier * roomFactor;
			
			//DynamicSoundFilters.getLogger().info(" => " + decayTime + " " + reflectionsGain + " " + reflectionsDelay + " " + lateReverbGain + " " + lateReverbDelay);
			
			// clamp values
			
			decayTime = MathHelper.clamp(decayTime, EXTEfx.AL_REVERB_MIN_DECAY_TIME, EXTEfx.AL_REVERB_MAX_DECAY_TIME);
			reflectionsGain = MathHelper.clamp(reflectionsGain, EXTEfx.AL_REVERB_MIN_REFLECTIONS_GAIN, EXTEfx.AL_REVERB_MAX_REFLECTIONS_GAIN);
			reflectionsDelay = MathHelper.clamp(reflectionsDelay, EXTEfx.AL_REVERB_MIN_REFLECTIONS_DELAY, EXTEfx.AL_REVERB_MAX_REFLECTIONS_DELAY);
			lateReverbGain = MathHelper.clamp(lateReverbGain, EXTEfx.AL_REVERB_MIN_LATE_REVERB_GAIN, EXTEfx.AL_REVERB_MAX_LATE_REVERB_GAIN);
			lateReverbDelay = MathHelper.clamp(lateReverbDelay, EXTEfx.AL_REVERB_MIN_LATE_REVERB_DELAY, EXTEfx.AL_REVERB_MAX_LATE_REVERB_DELAY);
			
			// apply values
			EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, 0);
			{
				EXTEfx.alEffecti(id, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_DENSITY, density);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_DIFFUSION, diffusion);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_GAIN, gain);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_GAINHF, gainHF);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_DECAY_TIME, decayTime);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_DECAY_HFRATIO, decayHFRatio);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, reflectionsGain);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_REFLECTIONS_DELAY, reflectionsDelay);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, lateReverbGain);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_LATE_REVERB_DELAY, lateReverbDelay);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_AIR_ABSORPTION_GAINHF, airAbsorptionGainHF);
				EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_ROOM_ROLLOFF_FACTOR, 0.0f); // automatically managed
				EXTEfx.alEffecti(id, EXTEfx.AL_REVERB_DECAY_HFLIMIT, decayHFLimit);
			}
			EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, id);
			EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, 1);
		}
	}

	private static boolean hasSkyAbove(ClientWorld world, BlockPos pos) {
		if(world.getDimension().hasCeiling()) return false;
		
		Chunk chunk = world.getChunk(pos);
		Heightmap heightMap = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING);
		int x = pos.getX() - chunk.getPos().getStartX();
		int z = pos.getZ() - chunk.getPos().getStartZ();
		x = Math.max(0, Math.min(x, 15));
		z = Math.max(0, Math.min(z, 15));
		return heightMap != null && heightMap.get(x, z) <= pos.getY();
	}
}
