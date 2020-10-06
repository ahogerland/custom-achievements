package com.customachievements.requirements;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

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
