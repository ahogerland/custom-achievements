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
package com.customachievements;

import com.customachievements.events.KilledNpc;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.NPCManager;
import net.runelite.client.task.Schedule;

import javax.inject.Inject;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class NpcKillReporter
{
	private final Client client;
	private final EventBus eventBus;
	private final NPCManager npcManager;

	private final CustomAchievementsConfig config;

	private final Set<NPC> targetNpcs;
	private final Set<NPC> ignoredNpcs;
	private HitsplatApplied lastHit;
	private boolean validateFullHealth;

	@Inject
	public NpcKillReporter(
			final Client client,
			final EventBus eventBus,
			final NPCManager npcManager,
			final CustomAchievementsConfig config)
	{
		this.client = client;
		this.eventBus = eventBus;
		this.npcManager = npcManager;
		this.config = config;

		targetNpcs = new HashSet<>();
		ignoredNpcs = new HashSet<>();
		lastHit = new HitsplatApplied();
		validateFullHealth = false;
	}

	@Subscribe
	public void onGameTick(final GameTick _gameTick)
	{
		if (validateFullHealth)
		{
			final NPC npc = (NPC) lastHit.getActor();
			final Integer maxHealth = npcManager.getHealth(npc.getId());
			final int originalHealth = getNpcHealth(npc) + lastHit.getHitsplat().getAmount();

			if (maxHealth != null && originalHealth == maxHealth)
			{
				ignoredNpcs.remove(npc);
				targetNpcs.add(npc);
			}

			validateFullHealth = false;
		}
	}

	@Subscribe
	public void onHitsplatApplied(final HitsplatApplied hitsplatApplied)
	{
		if (hitsplatApplied.getActor() instanceof NPC)
		{
			final Hitsplat hitsplat = hitsplatApplied.getHitsplat();
			final NPC npc = (NPC) hitsplatApplied.getActor();

			if (hitsplat.isMine() && hitsplat.getHitsplatType() != Hitsplat.HitsplatType.BLOCK_ME)
			{
				if (!ignoredNpcs.contains(npc))
				{
					targetNpcs.add(npc);
				}
				else if (npc.getHealthScale() == -1)
				{
					// NPC health bar was hidden, so check to see if health was full on next game tick
					// and add the NPC to the target set if that's the case.
					validateFullHealth = true;
				}

				lastHit = hitsplatApplied;
			}
			else if (config.ironmanModeEnabled() && hitsplat.isOthers() && hitsplat.getHitsplatType() != Hitsplat.HitsplatType.BLOCK_OTHER)
			{
				targetNpcs.remove(npc);
				ignoredNpcs.add(npc);
			}
		}
	}

	@Subscribe
	public void onActorDeath(final ActorDeath actorDeath)
	{
		if (actorDeath.getActor() instanceof NPC)
		{
			final NPC npc = (NPC) actorDeath.getActor();

			if (!ignoredNpcs.remove(npc) && targetNpcs.remove(npc))
			{
				eventBus.post(new KilledNpc(npc));
			}
		}
	}

	@Schedule(
			period = CustomAchievementsPlugin.UPDATE_FREQUENCY,
			unit = ChronoUnit.SECONDS
	)
	public void updateTask()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			update();
		}
	}

	public void update()
	{
		final List<NPC> npcs = client.getNpcs();

		final Predicate<NPC> canForgetNpc = npc -> {
			final boolean healthShowing = npc.getHealthScale() != -1;
			final boolean fullHealth = (npc.getHealthRatio() / npc.getHealthScale()) == 1;

			return (healthShowing && fullHealth) || !npcs.contains(npc);
		};

		targetNpcs.removeIf(canForgetNpc);
		ignoredNpcs.removeIf(canForgetNpc);
	}

	private int getNpcHealth(NPC npc)
	{
		final Integer maxHealth = npcManager.getHealth(npc.getId());
		final float ratio = npc.getHealthRatio();
		final float scale = npc.getHealthScale();

		// This is the equivalent of the reverse operation done by the server to get the health.
		// The calculation done for the health ratio by the server is as follows (quoted from the opponentinfo plugin):
		//     ratio = 1 + ((scale - 1) * health) / maxHealth, if health > 0, otherwise 0
		return maxHealth == null ? 0 : (int) Math.ceil((maxHealth * Math.max(0, ratio - 1)) / (scale - 1));
	}
}
