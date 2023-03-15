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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.openal.EXTEfx;

import me.andre111.dynamicsf.config.Config;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstance.AttenuationType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ObstructionFilter {
	private static int id = -1;
	
	private static boolean enabled = false;
	private static Map<SoundInstance, Float> obstructions = new HashMap<>();
	private static Queue<SoundInstance> toScan = new ConcurrentLinkedQueue<>();
	
	//TODO: configurable
	private static List<Material> HIGH_OBSTRUCTION_MATERIALS = Arrays.asList(Material.WOOL, Material.SPONGE);
	
	public static void reinit() {
		id = EXTEfx.alGenFilters();
	}
	
	public static void updateGlobal(MinecraftClient client) {
		LiquidFilter.updateGlobal(client);
		
		if(client.world != null && client.player != null && client.isRunning()) {
			update(client);
		} else {
			reset();
		}
	}
	
	public static boolean updateSoundInstance(SoundInstance soundInstance) {
		if(id == -1) {
			reinit();
		}
		
		// process liquid filter
		float lowPassGain = 1.0f;
		float lowPassGainHF = 1.0f;
		if(LiquidFilter.updateSoundInstance(soundInstance)) {
			lowPassGain = LiquidFilter.getLowPassGain();
			lowPassGainHF = LiquidFilter.getLowPassGainHF();
		}
		
		// process this filter
		if(enabled && soundInstance.getAttenuationType() == AttenuationType.LINEAR) {
			Float obstructionAmount = obstructions.get(soundInstance);
			if(obstructionAmount == null) {
				toScan.add(soundInstance);
				obstructionAmount = 0.0f;
			}
			
			if(obstructionAmount > 0.01) {
				lowPassGain = lowPassGain * (1.0f - obstructionAmount);
				lowPassGainHF = lowPassGainHF * (1.0f - MathHelper.sqrt(obstructionAmount));
			}
		}

        lowPassGain = Math.max(0, Math.min(lowPassGain, 1));
        lowPassGainHF = Math.max(0, Math.min(lowPassGainHF, 1));
		if(lowPassGain >= 1.0f && lowPassGainHF >= 1.0f) return false;
		
        EXTEfx.alFilteri(id, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
        EXTEfx.alFilterf(id, EXTEfx.AL_LOWPASS_GAIN, lowPassGain);
        EXTEfx.alFilterf(id, EXTEfx.AL_LOWPASS_GAINHF, lowPassGainHF);
        
        return true;
	}
	
	public static int getID() {
		return id;
	}
	
	private static void reset() {
		enabled = false;
		obstructions.clear();
	}

	private static void update(MinecraftClient client) {
		enabled = Config.getData().obstructionFilter.enabled;
		
		// remove finished / stopped sounds
		obstructions.entrySet().removeIf(entry -> !client.getSoundManager().isPlaying(entry.getKey()));
		
		// add new sounds
		SoundInstance newSoundInstance = null;
		while((newSoundInstance = toScan.poll()) != null) obstructions.put(newSoundInstance, 0.0f);
		
		// update sound obstructions
		for(Map.Entry<SoundInstance, Float> e : obstructions.entrySet()) {
			float currentAmount = e.getValue();
			float nextAmount = getObstructionAmount(client, e.getKey());
			e.setValue((currentAmount * 3.0f + nextAmount) / 4.0f);
		}
	}
	
	private static float getObstructionAmount(MinecraftClient client, SoundInstance soundInstance) {
		float obstructionStep = Config.getData().obstructionFilter.obstructionStep;
		float obstructionMax = Config.getData().obstructionFilter.obstructionMax;
		
		// determine and validate positions
		Vec3d playerPosD = client.player.getPos().add(0, client.player.getEyeHeight(client.player.getPose()), 0);
		Vec3d soundPosD = new Vec3d(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ());
		Vec3d currentPosD = soundPosD;
		if(Double.isNaN(playerPosD.x) || Double.isNaN(playerPosD.y) || Double.isNaN(playerPosD.z)) return 0;
		if(Double.isNaN(soundPosD.x) || Double.isNaN(soundPosD.y) || Double.isNaN(soundPosD.z)) return 0;
		BlockPos playerPos = BlockPos.ofFloored(playerPosD);
		BlockPos.Mutable currentPos = new BlockPos.Mutable(soundPosD.x, soundPosD.y, soundPosD.z);
		
		// check obstruction
		float obstruction = 0.0f;
		for(int i=0; i<100; i++) {
			// store previous positions and check for completion
			Vec3d prevPosD = currentPosD;
			if(playerPos.equals(currentPos)) return obstruction;
			
			// find smallest full block step "along line"
			boolean changeX = currentPos.getX() != playerPos.getX();
			boolean changeY = currentPos.getY() != playerPos.getY();
			boolean changeZ = currentPos.getZ() != playerPos.getZ();
			int nextBlockX = currentPos.getX() + (changeX ? (currentPos.getX() < playerPos.getX() ? 1 : -1) : 0);
			int nextBlockY = currentPos.getY() + (changeY ? (currentPos.getY() < playerPos.getY() ? 1 : -1) : 0);
			int nextBlockZ = currentPos.getZ() + (changeZ ? (currentPos.getZ() < playerPos.getZ() ? 1 : -1) : 0);
			double progressPercentX = changeX ? Math.abs((nextBlockX - soundPosD.x) / (playerPos.getX() - soundPosD.x)) : Double.POSITIVE_INFINITY;
			double progressPercentY = changeY ? Math.abs((nextBlockY - soundPosD.y) / (playerPos.getY() - soundPosD.y)) : Double.POSITIVE_INFINITY;
			double progressPercentZ = changeZ ? Math.abs((nextBlockZ - soundPosD.z) / (playerPos.getZ() - soundPosD.z)) : Double.POSITIVE_INFINITY;
			double progressPercent = Math.min(Math.min(progressPercentX, progressPercentY), progressPercentZ);
			
			// go to next block position
			currentPosD = new Vec3d(soundPosD.x + (playerPosD.x - soundPosD.x) * progressPercent, soundPosD.y + (playerPosD.y - soundPosD.y) * progressPercent, soundPosD.z + (playerPosD.z - soundPosD.z) * progressPercent);
			currentPos.set(currentPosD.x, currentPosD.y, currentPosD.z);
			
			// check for obstruction and add amount
			BlockState blockState = client.world.getBlockState(currentPos);
			if(blockState.isFullCube(client.world, currentPos)) {
				float newObstruction = HIGH_OBSTRUCTION_MATERIALS.contains(blockState.getMaterial()) ? obstructionStep * 2.0f : obstructionStep;
				obstruction += newObstruction * currentPosD.distanceTo(prevPosD);
				if(obstruction > obstructionMax) return obstructionMax;
			}
		}
		
		return obstruction;
	}
}
