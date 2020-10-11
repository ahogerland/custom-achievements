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

import java.awt.Color;
import java.util.Set;

import com.customachievements.Achievement;
import com.customachievements.CustomAchievementsConfig;
import com.customachievements.StatusListener;
import com.google.common.collect.ImmutableSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;

@Getter
@Setter
public abstract class Requirement
{
	protected static final Set<Character> VOWELS = ImmutableSet.of('a', 'e', 'i', 'o', 'u');
	protected static final String NAME_UNKNOWN = "???";

	protected RequirementType type;
	protected boolean complete;
	protected boolean inProgress;

	@Getter(AccessLevel.NONE)
	protected transient StatusListener statusListener;

	public Requirement(RequirementType type)
	{
		this(type, false);
	}

	public Requirement(RequirementType type, boolean complete)
	{
		this.type = type;
		this.complete = complete;
		this.inProgress = false;
	}

	public abstract Requirement deepCopy();

	public void refresh(Client client) {}

	public void reset()
	{
		complete = false;
		inProgress = false;
	}

	public Color getColor(Achievement parent, final CustomAchievementsConfig config)
	{
		if (complete)
		{
			return ColorScheme.PROGRESS_COMPLETE_COLOR;
		}
		else if (parent.isComplete() || parent.isForceComplete())
		{
			return ColorScheme.LIGHT_GRAY_COLOR;
		}
		else if (inProgress && config.requirementInProgressEnabled())
		{
			return ColorScheme.PROGRESS_INPROGRESS_COLOR;
		}
		else
		{
			return ColorScheme.PROGRESS_ERROR_COLOR;
		}
	}

	public String getStatus(Achievement parent, final CustomAchievementsConfig config)
	{
		if (complete)
		{
			return "Complete";
		}
		else if (parent.isComplete())
		{
			return "Skipped";
		}
		else if (inProgress && config.requirementInProgressEnabled())
		{
			return "In Progress";
		}
		else
		{
			return "Incomplete";
		}
	}

	@Override
	public String toString()
	{
		return NAME_UNKNOWN;
	}

	public void setStatusListener(StatusListener listener)
	{
		statusListener = listener;
	}

	protected void broadcastStatus()
	{
		if (complete)
		{
			broadcastComplete();
		}
		else if (inProgress)
		{
			broadcastUpdated();
		}
	}

	protected void broadcastComplete()
	{
		if (statusListener != null)
		{
			statusListener.onComplete();
		}
	}

	protected void broadcastUpdated()
	{
		if (statusListener != null)
		{
			statusListener.onUpdated();
		}
	}
}
