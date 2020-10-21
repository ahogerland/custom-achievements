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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.customachievements.AchievementState.*;

@Getter
@Setter
public abstract class AchievementElement
{
	private AchievementState state;
	private boolean forceComplete;
	private boolean uiExpanded;

	@Setter(AccessLevel.NONE)
	private final List<AchievementElement> children;

	@Getter(AccessLevel.NONE)
	private transient AchievementStateListener stateListener;

	public AchievementElement()
	{
		this.state = AchievementState.INCOMPLETE;
		this.forceComplete = false;
		this.uiExpanded = true;
		this.children = new ArrayList<>();
		this.stateListener = null;
	}

	public AchievementElement(AchievementElement other)
	{
		this.state = other.state;
		this.forceComplete = other.forceComplete;
		this.uiExpanded = other.uiExpanded;
		this.children = new ArrayList<>();
		this.stateListener = null;

		for (AchievementElement entry : other.children)
		{
			this.children.add(entry.deepCopy());
		}
	}

	public abstract void refresh();
	public abstract AchievementElement deepCopy();
	public abstract String completionChatMessage(CustomAchievementsConfig config);
	public abstract String toString();

	public void reset()
	{
		setState(INCOMPLETE);
		setForceComplete(false);
	}

	public void click()
	{
		setForceComplete(!isForceComplete());
	}

	public void addChild(AchievementElement child)
	{
		children.add(child);
	}

	public void removeChild(AchievementElement child)
	{
		children.remove(child);
	}

	public void setChild(int index, AchievementElement child)
	{
		children.set(index, child);
	}

	public AchievementElement getChild(int index)
	{
		return children.get(index);
	}

	public AchievementState getChildrenState()
	{
		boolean complete = true;
		boolean inProgress = false;

		for (AchievementElement child : children)
		{
			switch (child.getState())
			{
				case INCOMPLETE:
					complete = false;
					break;
				case IN_PROGRESS:
					complete = false;
				case COMPLETE:
					inProgress = true;
			}
		}

		if (complete)
		{
			return COMPLETE;
		}
		else if (inProgress)
		{
			return IN_PROGRESS;
		}
		else
		{
			return INCOMPLETE;
		}
	}

	public void setState(AchievementState state)
	{
		AchievementState old = this.state;
		this.state = state;

		if (old != state)
		{
			broadcastState();
		}
	}

	protected void broadcastState()
	{
		if (stateListener != null)
		{
			stateListener.onStateChanged(state);
		}
	}
}
