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

import me.andre111.dynamicsf.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class LiquidFilter {
	private static boolean enabled = false;
    private static float lowPassGain = 1.0f;
    private static float lowPassGainHF = 1.0f;
    private static float targetLowPassGain = 1.0f;
    private static float targetLowPassGainHF = 1.0f;

	public static void updateGlobal(MinecraftClient client) {
		if(client.world != null && client.player != null && client.isRunning()) {
			update(client);
		} else {
			reset();
		}
	}
	
	public static boolean updateSoundInstance(SoundInstance soundInstance) {
		if(!enabled) return false;

        lowPassGain = Math.max(0, Math.min(lowPassGain, 1));
        lowPassGainHF = Math.max(0, Math.min(lowPassGainHF, 1));
        if(lowPassGain >= 1 && lowPassGainHF >= 1) return false;
		
		return true;
	}
	
	public static float getLowPassGain() {
		return lowPassGain;
	}

	public static float getLowPassGainHF() {
		return lowPassGainHF;
	}
	
	private static void reset() {
		enabled = false;
        lowPassGain = 1.0f;
        lowPassGainHF = 1.0f;
        targetLowPassGain = 1.0f;
        targetLowPassGainHF = 1.0f;
	}

	private static void update(MinecraftClient client) {
		enabled = Config.getData().liquidFilter.enabled;
		
		if(enabled) {
			BlockPos playerPos = new BlockPos(
            		MathHelper.floor(client.player.getPos().getX()), 
            		MathHelper.floor(client.player.getPos().getY() + client.player.getEyeHeight(client.player.getPose())), 
            		MathHelper.floor(client.player.getPos().getZ())
            		);
			
			// update target values
			FluidState fluidState = client.world.getFluidState(playerPos);
			if(fluidState.isIn(FluidTags.WATER)) {
                targetLowPassGain = Config.getData().liquidFilter.waterGain;
                targetLowPassGainHF = Config.getData().liquidFilter.waterGainHF;
			} else if(fluidState.isIn(FluidTags.LAVA)) {
                targetLowPassGain = Config.getData().liquidFilter.lavaGain;
                targetLowPassGainHF = Config.getData().liquidFilter.lavaGainHF;
			} else {
                targetLowPassGain = 1.0f;
                targetLowPassGainHF = 1.0f;
			}
			
			// interpolate values
            lowPassGain = (targetLowPassGain + lowPassGain) / 2.0f;
            lowPassGainHF = (targetLowPassGainHF + lowPassGainHF) / 2.0f;
		}
	}
}
