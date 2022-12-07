/*
 * Copyright (c) 2022 Andre Schweiger
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
package me.andre111.dynamicsf;

import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import me.andre111.dynamicsf.config.Config;
import me.andre111.dynamicsf.filter.ObstructionFilter;
import me.andre111.dynamicsf.filter.ReverbFilter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;

@Environment(EnvType.CLIENT)
public class FilterManager {
	public void updateGlobal(MinecraftClient client) {
		ReverbFilter.updateGlobal(client);
		ObstructionFilter.updateGlobal(client);
	}
	
	public void updateSoundInstance(SoundInstance soundInstance, int sourceID) {
		if(Config.getData().general.isIgnoredSoundEvent(soundInstance.getId())) return;
		
		boolean includeReverb = ReverbFilter.updateSoundInstance(soundInstance);
		boolean includeLowPass = ObstructionFilter.updateSoundInstance(soundInstance);
		
		AL11.alSourcei(sourceID, EXTEfx.AL_DIRECT_FILTER, includeLowPass ? ObstructionFilter.getID() : 0);
		AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, includeReverb ? ReverbFilter.getSlot() : 0, 0, includeLowPass ? ObstructionFilter.getID() : 0);
		
		// retry once on error
		if(AL11.alGetError() != AL11.AL_NO_ERROR) {
			ReverbFilter.reinit();
			ObstructionFilter.reinit();

			AL11.alSourcei(sourceID, EXTEfx.AL_DIRECT_FILTER, includeLowPass ? ObstructionFilter.getID() : 0);
			AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, includeReverb ? ReverbFilter.getSlot() : 0, 0, includeLowPass ? ObstructionFilter.getID() : 0);
		}
		
		// report further errors
		int error = AL11.alGetError();
		if(error != AL11.AL_NO_ERROR) {
			System.err.println("OpenAL error when applying sound filters: "+error);
		}
	}
}
