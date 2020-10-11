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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.customachievements.requirements.Requirement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;

@Getter
@Setter
public class Achievement
{
	private String name;
	private boolean complete;
	private boolean inProgress;
	private boolean forceComplete;
	private boolean autoCompleted;
	private boolean uiExpanded;

	@Getter(AccessLevel.NONE)
	private transient StatusListener statusListener;

	private final List<Requirement> requirements;

	public Achievement(String name)
	{
		this.name = name;
		this.complete = false;
		this.inProgress = false;
		this.forceComplete = false;
		this.autoCompleted = true;
		this.uiExpanded = true;
		this.requirements = new ArrayList<>();
	}

	public Achievement(Achievement other)
	{
		this.name = other.name;
		this.complete = other.complete;
		this.inProgress = other.inProgress;
		this.forceComplete = other.forceComplete;
		this.autoCompleted = other.autoCompleted;
		this.uiExpanded = other.uiExpanded;
		this.requirements = new ArrayList<>();

		for (Requirement requirement : other.requirements)
		{
			this.requirements.add(requirement.deepCopy());
		}
	}

	public void addRequirement(Requirement requirement)
	{
		requirements.add(requirement);
	}

	public void removeRequirement(Requirement requirement)
	{
		requirements.remove(requirement);
	}

	public void refresh(Client client)
	{
		for (Requirement requirement : requirements)
		{
			requirement.refresh(client);
		}
	}

	public void checkStatus()
	{
		boolean completeStatus = true;
		inProgress = false;

		for (Requirement requirement : requirements)
		{
			if (!requirement.isComplete())
			{
				completeStatus = false;
			}

			if (requirement.isComplete() || requirement.isInProgress())
			{
				inProgress = true;
			}
		}

		if (autoCompleted && !complete && completeStatus)
		{
			// TODO: Add a config setting to enable this when auto complete is disabled.
			complete = true;
			broadcastStatus();
		}
		else
		{
			complete = completeStatus;
		}

		if (complete)
		{
			forceComplete = false;
		}
	}

	public Color getColor(final CustomAchievementsConfig config)
	{
		if (complete || forceComplete)
		{
			return ColorScheme.PROGRESS_COMPLETE_COLOR;
		}
		else if (inProgress && config.achievementInProgressEnabled())
		{
			return ColorScheme.PROGRESS_INPROGRESS_COLOR;
		}
		else
		{
			return ColorScheme.PROGRESS_ERROR_COLOR;
		}
	}

	public String getToolTip(final CustomAchievementsConfig config)
	{
		if (forceComplete)
		{
			return "Complete (Forced)";
		}
		else if (complete)
		{
			return "Complete";
		}
		else if (inProgress && config.achievementInProgressEnabled())
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
		return String.format("%s%s", name, forceComplete ? " *" : "");
	}

	public void setStatusListener(StatusListener listener)
	{
		this.statusListener = listener;
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
