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

import com.customachievements.events.KilledNpc;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.eventbus.Subscribe;

import static com.customachievements.AchievementState.*;

@Getter
@Setter
public class SlayRequirement extends Requirement
{
	private String name;
	private boolean properNoun;
	private int quantity;
	private int count;

	public SlayRequirement(String name, boolean properNoun, int quantity)
	{
		super(RequirementType.SLAY);
		this.name = name;
		this.properNoun = properNoun;
		this.quantity = quantity;
		this.count = 0;
	}

	public SlayRequirement(SlayRequirement other)
	{
		super(other);
		this.name = other.name;
		this.properNoun = other.properNoun;
		this.quantity = other.quantity;
		this.count = other.count;
	}

	@Subscribe
	public void onKilledNpc(final KilledNpc killedNpc)
	{
		final NPC npc = killedNpc.getNpc();

		if (getProgress() != COMPLETE && name.equalsIgnoreCase(npc.getName()))
		{
			count++;
			updateState();
			broadcastState();
		}
	}

	@Override
	public void forceUpdate(Client client)
	{
		updateState();
	}

	@Override
	public Requirement deepCopy()
	{
		return new SlayRequirement(this);
	}

	@Override
	public void reset()
	{
		super.reset();
		count = 0;
	}

	@Override
	public String toString()
	{
		if (name.isEmpty())
		{
			return NAME_UNKNOWN;
		}
		else
		{
			final String article = VOWELS.contains(Character.toLowerCase(name.charAt(0))) ? "an " : "a ";
			final int done = getState() == COMPLETE ? quantity : Math.min(count, quantity);

			return String.format("Defeat %s%s (%d/%d)", !isProperNoun() ? article : "", name, done, quantity);
		}
	}

	private void updateState()
	{
		if (count >= quantity)
		{
			setProgress(COMPLETE);
		}
		else if (count > 0)
		{
			setProgress(IN_PROGRESS);
		}
		else
		{
			setProgress(INCOMPLETE);
		}
	}
}
