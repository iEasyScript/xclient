/*
 * Copyright (c) 2018 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.projectx.questhelper.helpers.mischelpers.farmruns;

import java.util.Collections;
import java.util.HashSet;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.Set;
import org.jetbrains.annotations.NotNull;

@Getter
public class FarmingRegion implements Comparable<FarmingRegion>
{
	private final String name;
	private final int regionID;
	private final boolean definite;
	private final FarmingPatch[] patches;
	private final Set<Integer> regionIDs;

	FarmingRegion(String name, int regionID, boolean definite, FarmingPatch... patches)
	{
		this.name = name;
		this.regionID = regionID;
		this.definite = definite;
		this.patches = patches;
		this.regionIDs = Set.of(regionID);
		for (FarmingPatch p : patches)
		{
			p.setRegion(this);
		}
	}

	FarmingRegion(String name, int regionID, boolean definite, Set<Integer> regionIDs, FarmingPatch... patches)
	{
		this.name = name;
		this.regionID = regionID;
		this.definite = definite;
		this.patches = patches;

		Set<Integer> allRegionIds = new HashSet<>(regionIDs);
		allRegionIds.add(regionID);
		this.regionIDs = Collections.unmodifiableSet(allRegionIds);

		for (FarmingPatch p : patches)
		{
			p.setRegion(this);
		}
	}

	/**
	 * Check if the given WorldPoint is within this farming region
	 * Checks if the point's regionID is in this region's regionIDs set
	 *
	 * @param loc The WorldPoint to check
	 * @return true if the point is within this farming region
	 */
	public boolean isInBounds(WorldPoint loc)
	{
		return regionIDs.contains(loc.getRegionID());
	}

	@Override
	public String toString()
	{
		String sb = "FarmingRegion{name='" + name + '\'' +
				", regionID=" + regionID +
				", definite=" + definite +
				", patches=" + patches.length +
				'}';
		return sb;
	}

	@Override
	public int compareTo(@NotNull FarmingRegion o)
	{
		return Integer.compare(regionID, o.getRegionID());
	}
}