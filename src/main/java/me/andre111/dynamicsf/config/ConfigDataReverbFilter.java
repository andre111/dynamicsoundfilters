/*
 * Copyright (c) 2021 Andre Schweiger
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
package me.andre111.dynamicsf.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.andre111.dynamicsf.filter.ReverbFilter;
import me.andre111.dynamicsf.filter.ReverbFilter.Reverb;
import net.minecraft.util.Identifier;

public class ConfigDataReverbFilter {
	public boolean enabled = true;
	
	public float reverbPercent = 1.0f;
	
	public int maxBlocks = 1024;
	public boolean checkSky = true;
	
	public List<String> dimensionBaseReverb = Arrays.asList("minecraft:the_nether;1.0");
	public List<String> customBlockReverb = new ArrayList<>();
	
	public float density = 0.2f;
	public float diffusion = 0.6f;
	public float gain = 0.15f;
	public float gainHF = 0.8f;
	public float minDecayTime = 0.1f;
	public float decayHFRatio = 0.7f;
	public float airAbsorptionGainHF = 0.99f;
	public float reflectionsGainBase = 0.05f;
	public float reflectionsGainMultiplier = 0.05f;
	public float reflectionsDelayMultiplier = 0.025f;
	public float lateReverbGainBase = 1.26f;
	public float lateReverbGainMultiplier = 0.1f;
	public float lateReverbDelayMultiplier = 0.01f;
	
	
	//-----------------------------------------------------------------------
	private transient boolean cached = false;
	private transient Map<Identifier, Float> dimensionBaseReverbMap = new HashMap<>();
	private transient Map<Identifier, ReverbFilter.Reverb> customBlockReverbMap = new HashMap<>();
	
	private void calculateCache() {
		if(!cached) {
			dimensionBaseReverbMap = ConfigHelper.parseToMap(dimensionBaseReverb, Identifier::new, Float::parseFloat);
			customBlockReverbMap = ConfigHelper.parseToMap(customBlockReverb, Identifier::new, ReverbFilter.Reverb::fromName);
			
			cached = true;
		}
	}
	
	public void recalculateCache() {
		cached = false;
		calculateCache();
	}
	
	public float getDimensionBaseReverb(Identifier dimension) {
		calculateCache();
		
		if(dimensionBaseReverbMap.containsKey(dimension)) {
			return dimensionBaseReverbMap.get(dimension);
		}
		
		return 0f;
	}
	
	public Reverb getCustomBlockReverb(Identifier block) {
		calculateCache();
		
		return customBlockReverbMap.get(block);
	}
}
