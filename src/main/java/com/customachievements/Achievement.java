package com.customachievements;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.customachievements.requirements.Requirement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;

@Getter
@Setter
public class Achievement
{
	private String name;
	private boolean complete;
	private boolean autoCompleted;
	private boolean uiExpanded;

	@Getter(AccessLevel.NONE)
	private transient CompleteListener listener;
	private transient boolean inProgress;

	private final List<Requirement> requirements;

	public Achievement(final String name)
	{
		this.name = name;
		this.complete = false;
		this.inProgress = false;
		this.autoCompleted = true;
		this.uiExpanded = true;
		this.requirements = new ArrayList<>();
	}

	public Achievement(Achievement other)
	{
		this.name = other.name;
		this.complete = other.complete;
		this.inProgress = other.inProgress;
		this.autoCompleted = other.autoCompleted;
		this.uiExpanded = other.uiExpanded;
		this.requirements = new ArrayList<>();

		for (Requirement requirement : other.requirements)
		{
			this.requirements.add(requirement.deepCopy());
		}
	}

	public void setCompleteListener(CompleteListener listener)
	{
		this.listener = listener;
	}

	public void addRequirement(Requirement requirement)
	{
		requirements.add(requirement);
	}

	public void removeRequirement(Requirement requirement)
	{
		requirements.remove(requirement);
	}

	public void update()
	{
		boolean completeStatus = true;
		boolean inProgressStatus = false;

		for (Requirement requirement : requirements)
		{
			if (!requirement.isComplete())
			{
				completeStatus = false;
			}
			else
			{
				inProgressStatus = true;
			}
		}

		if (autoCompleted)
		{
			// TODO: Add a config setting to enable this when auto complete is disabled.
			if (!complete && completeStatus)
			{
				complete = true;
				notifyListener();
			}
			else
			{
				complete = completeStatus;
			}
		}

		inProgress = inProgressStatus;
	}

	public Color getColor(final CustomAchievementsConfig config)
	{
		if (complete)
		{
			return ColorScheme.PROGRESS_COMPLETE_COLOR;
		}
		else if (inProgress && config.inProgressEnabled())
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
		if (complete)
		{
			return "Complete";
		}
		else if (inProgress && config.inProgressEnabled())
		{
			return "In Progress";
		}
		else
		{
			return "Incomplete";
		}
	}

	private void notifyListener()
	{
		listener.onComplete();
	}
}
