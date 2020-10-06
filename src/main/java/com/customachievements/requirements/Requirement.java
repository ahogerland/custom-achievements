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

import com.customachievements.Achievement;
import com.customachievements.CompleteListener;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;

@Getter
@Setter
public abstract class Requirement
{
	protected RequirementType type;
	protected boolean complete;

	@Getter(AccessLevel.NONE)
	private transient CompleteListener listener;

	public Requirement(final RequirementType type)
	{
		this(type, false);
	}

	public Requirement(final RequirementType type, final boolean complete)
	{
		this.type = type;
		this.complete = complete;
	}

	/**
	 * Return a deep copy of this Requirement.
	 */
	public abstract Requirement deepCopy();

	/**
	 * Update Requirement status outside normal events.
	 */
	public abstract void forceUpdate(Client client);

	public void setCompleteListener(CompleteListener listener)
	{
		this.listener = listener;
	}

	public Color getColor(Achievement parent)
	{
		if (complete)
		{
			return ColorScheme.PROGRESS_COMPLETE_COLOR;
		}
		else if (parent.isComplete())
		{
			return ColorScheme.LIGHT_GRAY_COLOR;
		}
		else
		{
			return ColorScheme.PROGRESS_ERROR_COLOR;
		}
	}

	public String getToolTip(Achievement parent)
	{
		if (complete)
		{
			return "Complete";
		}
		else if (parent.isComplete())
		{
			return "Skipped";
		}
		else
		{
			return "Incomplete";
		}
	}

	protected void notifyListener()
	{
		listener.onComplete();
	}
}
