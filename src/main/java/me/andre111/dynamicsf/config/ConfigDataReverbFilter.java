/*
 * Copyright (c) 2020 André Schweiger
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

public class ConfigDataReverbFilter {
	public boolean enabled = true;
	
	public float reverbPercent = 1.0f;
	
	public int maxBlocks = 1024;
	public boolean checkSky = true;
	
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
}
