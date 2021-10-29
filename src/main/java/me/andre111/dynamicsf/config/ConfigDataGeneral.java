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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.util.Identifier;

public class ConfigDataGeneral {
	public static final List<String> DEFAULT_IGNORED_SOUND_EVENTS = Collections.unmodifiableList(Arrays.asList(
			"minecraft:music.creative", "minecraft:music.credits", "minecraft:music.dragon", 
			"minecraft:music.end", "minecraft:music.game", "minecraft:music.menu", "minecraft:music.nether.basalt_deltas", 
			"minecraft:music.nether.nether_wastes", "minecraft:music.nether.soul_sand_valley", "minecraft:music.nether.crimson_forest", 
			"minecraft:music.nether.warped_forest", "minecraft:music.under_water", 
			
			"minecraft:ui.button.click", "minecraft:ui.toast.challenge_complete", "minecraft:ui.toast.in", "minecraft:ui.toast.out"));
	
	public List<String> ignoredSoundEvents = DEFAULT_IGNORED_SOUND_EVENTS;
	
	
	//-----------------------------------------------------------------------
	private transient boolean cached = false;
	private transient Set<Identifier> ignoredSoundEventsSet = Collections.emptySet();
	
	private void calculateCache() {
		if(!cached) {
			ignoredSoundEventsSet = ConfigHelper.parseToSet(ignoredSoundEvents, Identifier::new);
			
			cached = true;
		}
	}
	
	public void recalculateCache() {
		cached = false;
		calculateCache();
	}
	
	public boolean isIgnoredSoundEvent(Identifier identifier) {
		calculateCache();
		return ignoredSoundEventsSet.contains(identifier);
	}
}
