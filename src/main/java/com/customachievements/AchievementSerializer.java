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

import com.customachievements.requirements.AbstractRequirement;
import com.customachievements.requirements.ItemRequirement;
import com.customachievements.requirements.QuestRequirement;
import com.customachievements.requirements.Requirement;
import com.customachievements.requirements.RequirementType;
import com.customachievements.requirements.SkillRequirement;
import com.customachievements.requirements.SlayRequirement;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AchievementSerializer
{
	private final GsonBuilder builder;
	private final Type achievementListType;

	public AchievementSerializer()
	{
		this.builder = new GsonBuilder();
		this.achievementListType = new TypeToken<List<Achievement>>() {}.getType();

		builder.registerTypeAdapter(achievementListType, new AchievementListDeserializer());
	}

	public AchievementSerializer setPrettyPrinting()
	{
		builder.setPrettyPrinting();
		return this;
	}

	public String toJson(List<Achievement> achievements)
	{
		return builder.create().toJson(achievements);
	}

	public List<Achievement> fromJson(String json)
	{
		try
		{
			return builder.create().fromJson(json, achievementListType);
		}
		catch (JsonParseException e)
		{
			log.error(e.getMessage());
			return null;
		}
	}

	private static class AchievementListDeserializer implements JsonDeserializer<List<Achievement>>
	{
		@Override
		public List<Achievement> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			final List<Achievement> achievements = new ArrayList<>();
			final JsonArray jsonArray = json.getAsJsonArray();

			for (JsonElement jsonAchievementElement : jsonArray)
			{
				JsonObject jsonAchievement = jsonAchievementElement.getAsJsonObject();

				JsonElement name = jsonAchievement.get("name");
				JsonElement complete = jsonAchievement.get("complete");
				JsonElement inProgress = jsonAchievement.get("inProgress");
				JsonElement forceComplete = jsonAchievement.get("forceComplete");
				JsonElement autoCompleted = jsonAchievement.get("autoCompleted");
				JsonElement uiExpanded = jsonAchievement.get("uiExpanded");

				if (name == null || complete == null || inProgress == null ||
					forceComplete == null || autoCompleted == null || uiExpanded == null)
				{
					throw new JsonParseException("Invalid Achievement JSON");
				}

				Achievement achievement = new Achievement(name.getAsString());
				achievement.setComplete(complete.getAsBoolean());
				achievement.setInProgress(inProgress.getAsBoolean());
				achievement.setForceComplete(forceComplete.getAsBoolean());
				achievement.setAutoCompleted(autoCompleted.getAsBoolean());
				achievement.setUiExpanded(uiExpanded.getAsBoolean());

				for (JsonElement jsonRequirementElement : jsonAchievement.get("requirements").getAsJsonArray())
				{
					achievement.addRequirement(deserializeRequirement(jsonRequirementElement, context));
				}

				achievements.add(achievement);
			}

			return achievements;
		}

		private Requirement deserializeRequirement(JsonElement json, JsonDeserializationContext context) throws JsonParseException
		{
			RequirementType type = context.deserialize(json.getAsJsonObject().get("type"), RequirementType.class);

			switch (type)
			{
				case SKILL:
					return context.deserialize(json, SkillRequirement.class);
				case ITEM:
					return context.deserialize(json, ItemRequirement.class);
				case SLAY:
					return context.deserialize(json, SlayRequirement.class);
				case QUEST:
					return context.deserialize(json, QuestRequirement.class);
				case ABSTRACT:
				default:
					return context.deserialize(json, AbstractRequirement.class);
			}
		}
	}
}
