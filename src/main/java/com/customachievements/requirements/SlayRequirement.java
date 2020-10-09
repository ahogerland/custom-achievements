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

@Getter
@Setter
public class SlayRequirement extends Requirement
{
	private String name;
	private int quantity;
	private int count;

	public SlayRequirement(String name, int quantity)
	{
		this(name, quantity, 0, false);
	}

	public SlayRequirement(String name, int quantity, int count, boolean complete)
	{
		super(RequirementType.SLAY, complete);
		this.name = name;
		this.quantity = quantity;
		this.count = count;
	}

	@Subscribe
	public void onKilledNpc(final KilledNpc killedNpc)
	{
		final NPC npc = killedNpc.getNpc();

		if (!complete && name.equalsIgnoreCase(npc.getName()))
		{
			count++;
			checkStatus();
			broadcastStatus();
		}
	}

	@Override
	public Requirement deepCopy()
	{
		return new SlayRequirement(name, quantity, count, complete);
	}

	@Override
	public void refresh(Client client)
	{
		checkStatus();
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
			return super.toString();
		}
		else
		{
			return String.format("Defeat %s %s (%d/%d)",
					VOWELS.contains(Character.toLowerCase(name.charAt(0))) ? "an" : "a",
					name,
					complete ? quantity : Math.min(count, quantity),
					quantity);
		}
	}

	private void checkStatus()
	{
		if (count >= quantity)
		{
			complete = true;
		}

		inProgress = (count > 0);
	}
}
