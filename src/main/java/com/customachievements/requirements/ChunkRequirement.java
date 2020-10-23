/*
 * Copyright (c) 2020, Alec Hogerland <https://github.com/ahogerland>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.customachievements.requirements;

import com.customachievements.AchievementElement;
import com.customachievements.events.ChunkEntered;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.eventbus.Subscribe;

import static com.customachievements.AchievementState.COMPLETE;

@Getter
@Setter
public class ChunkRequirement extends Requirement
{
	private int regionId;
	private String nickname;

	public ChunkRequirement(int regionId, String nickname)
	{
		super(RequirementType.CHUNK);
		this.regionId = regionId;
		this.nickname = nickname;
	}

	public ChunkRequirement(ChunkRequirement other)
	{
		super(other);
		this.regionId = other.regionId;
		this.nickname = other.nickname;
	}

	@Subscribe
	public void onChunkEntered(final ChunkEntered chunkEntered)
	{
		if (getProgress() != COMPLETE && chunkEntered.getRegionId() == regionId)
		{
			setProgress(COMPLETE);
			refresh();
		}
	}

	@Override
	public void forceUpdate(Client client)
	{
		if (client.getGameState() == GameState.LOGGED_IN &&
			client.getLocalPlayer().getWorldLocation().getRegionID() == regionId)
		{
			setProgress(COMPLETE);
		}
	}

	@Override
	public AchievementElement deepCopy()
	{
		return new ChunkRequirement(this);
	}

	@Override
	public String toString()
	{
		if (Strings.isNullOrEmpty(nickname))
		{
			return String.format("Unlock Chunk %d", regionId);
		}
		else
		{
			return "Unlock " + nickname;
		}
	}
}
