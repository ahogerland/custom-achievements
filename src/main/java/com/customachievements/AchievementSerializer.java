package com.customachievements;

import com.customachievements.requirements.AbstractRequirement;
import com.customachievements.requirements.Requirement;
import com.customachievements.requirements.RequirementType;
import com.customachievements.requirements.SkillRequirement;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
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
				JsonElement autoCompleted = jsonAchievement.get("autoCompleted");
				JsonElement uiExpanded = jsonAchievement.get("uiExpanded");

				if (name == null || complete == null || autoCompleted == null || uiExpanded == null)
				{
					throw new JsonParseException("Invalid Achievement JSON");
				}

				Achievement achievement = new Achievement(name.getAsString());
				achievement.setComplete(complete.getAsBoolean());
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

		// TODO: Fill these in once each type is available
		private Requirement deserializeRequirement(JsonElement json, JsonDeserializationContext context) throws JsonParseException
		{
			RequirementType type = context.deserialize(json.getAsJsonObject().get("type"), RequirementType.class);

			switch (type)
			{
				case SKILL:
					return context.deserialize(json, SkillRequirement.class);
				case ITEM:
				case QUEST:
				case CHUNK:
				case ABSTRACT:
				default:
					return context.deserialize(json, AbstractRequirement.class);
			}
		}
	}
}
