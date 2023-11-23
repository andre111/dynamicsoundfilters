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
package me.andre111.dynamicsf.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ConfigDataObstructionFilter {
	public static final List<Identifier> DEFAULT_HIGH_OBSTRUCTION_BLOCKS = ofBlocks(
			Blocks.BLACK_WOOL, Blocks.BLUE_WOOL, Blocks.BROWN_WOOL, Blocks.CYAN_WOOL, Blocks.GRAY_WOOL, 
			Blocks.GREEN_WOOL, Blocks.LIGHT_BLUE_WOOL, Blocks.LIGHT_GRAY_WOOL, Blocks.LIME_WOOL, Blocks.MAGENTA_WOOL, 
			Blocks.ORANGE_WOOL, Blocks.PINK_WOOL, Blocks.PURPLE_WOOL, Blocks.RED_WOOL, Blocks.WHITE_WOOL, 
			Blocks.YELLOW_WOOL,
			
			Blocks.SPONGE, Blocks.WET_SPONGE
	);
	private static List<Identifier> ofBlocks(Block... blocks) {
		List<Identifier> list = new ArrayList<>();
		for(Block block : blocks) list.add(Registries.BLOCK.getId(block));
		return list;
	}
	
	public boolean enabled = true;
	
	public List<Identifier> highObstructionBlocks = new ArrayList<>(DEFAULT_HIGH_OBSTRUCTION_BLOCKS);
	
	public float obstructionStep = 0.1f;
	public float obstructionMax = 0.98f;
}
