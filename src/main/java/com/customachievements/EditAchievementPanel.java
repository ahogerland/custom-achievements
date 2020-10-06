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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.MatteBorder;

import com.customachievements.requirements.AbstractRequirement;
import com.customachievements.requirements.Requirement;
import com.customachievements.requirements.RequirementType;
import com.customachievements.requirements.SkillRequirement;
import com.customachievements.requirements.SkillTargetType;
import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

public class EditAchievementPanel extends JPanel
{
	public static final String ACTION_UPDATE = "ACTION_UPDATE";
	public static final String ACTION_CANCEL = "ACTION_CANCEL";

	private static final ImageIcon ADD_ICON;

	private final List<ActionListener> listeners = new ArrayList<>();

	private final CustomAchievementsPlugin plugin;
	private final CustomAchievementsConfig config;

	@Getter
	private Achievement target;
	private Achievement dummy;
	private int dropdownIndex = 0;

	static
	{
		final BufferedImage addImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "add_icon.png");
		ADD_ICON = new ImageIcon(addImage);
	}

	EditAchievementPanel(final CustomAchievementsPlugin plugin, final CustomAchievementsConfig config, final Achievement target)
	{
		this.plugin = plugin;
		this.config = config;
		this.target = target;
		this.dummy = new Achievement(target);

		setLayout(new GridBagLayout());

		refresh();
	}

	public void setTarget(final Achievement target)
	{
		this.target = target;
		this.dummy = new Achievement(target);

		refresh();
	}

	public void updateTarget()
	{
		target.setName(dummy.getName());
		target.setAutoCompleted(dummy.isAutoCompleted());

		plugin.removeAllRequirements(target);
		plugin.addAllRequirements(target, dummy.getRequirements());
		plugin.addAchievement(target); // Required for new Achievements

		// Update Achievement status
		target.update();
		plugin.updateConfig();

		notifyListeners(ACTION_UPDATE);
	}

	public void refresh()
	{
		removeAll();

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.gridx = 0;
		gbc.gridy = 0;

		final JLabel topSeparator = new JLabel();
		final JLabel bottomSeparator = new JLabel();
		final MatteBorder separatorBorder = BorderFactory.createMatteBorder(4, 0, 4, 0, ColorScheme.DARK_GRAY_COLOR);

		topSeparator.setBorder(separatorBorder);
		bottomSeparator.setBorder(separatorBorder);

		add(createAchievementNamePanel(), gbc);
		gbc.gridy++;

		add(createAutoCompletionCheckBox(), gbc);
		gbc.gridy++;

		add(topSeparator, gbc);
		gbc.gridy++;

		for (Requirement requirement : dummy.getRequirements())
		{
			add(createRequirementPanel(requirement), gbc);
			gbc.gridy++;
		}

		if (!dummy.getRequirements().isEmpty())
		{
			add(bottomSeparator, gbc);
			gbc.gridy++;
		}

		add(createAddRequirementPanel(), gbc);
		gbc.gridy++;

		add(createConfirmationPanel(), gbc);

		repaint();
		revalidate();
	}

	public void addActionListener(@NonNull final ActionListener listener)
	{
		listeners.add(listener);
	}

	private void notifyListeners(final String command)
	{
		ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);

		for (ActionListener listener : listeners)
		{
			listener.actionPerformed(event);
		}
	}

	private JPanel createAchievementNamePanel()
	{
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JLabel nameLabel = new JLabel("Achievement Name");
		final FlatTextField nameInput = new FlatTextField();

		nameLabel.setForeground(ColorScheme.BRAND_ORANGE);
		nameLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());

		nameInput.setText(dummy.getName());
		nameInput.setBackground(ColorScheme.DARK_GRAY_COLOR);
		nameInput.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		nameInput.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e)
			{
				dummy.setName(nameInput.getText());
			}
		});

		wrapper.add(nameLabel, BorderLayout.NORTH);
		wrapper.add(nameInput, BorderLayout.CENTER);

		return wrapper;
	}

	private JPanel createAutoCompletionCheckBox()
	{
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final String toolTip = "Automatically mark as completed once all requirements are complete.";

		final JLabel nameLabel = new JLabel("Automatic Completion");
		nameLabel.setForeground(ColorScheme.BRAND_ORANGE);
		nameLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setToolTipText(toolTip);

		final JCheckBox checkBox = new JCheckBox();
		checkBox.setToolTipText(toolTip);
		checkBox.setSelected(dummy.isAutoCompleted());
		checkBox.addActionListener(e -> dummy.setAutoCompleted(checkBox.isSelected()));

		wrapper.add(nameLabel, BorderLayout.WEST);
		wrapper.add(checkBox, BorderLayout.EAST);

		return wrapper;
	}

	private JPanel createRequirementPanel(final Requirement requirement)
	{
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JLabel nameLabel = new JLabel(String.format("%s %s", requirement.getType(), "Requirement"));
		nameLabel.setForeground(ColorScheme.BRAND_ORANGE);
		nameLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());

		JPanel requirementPanel;

		switch (requirement.getType())
		{
			case SKILL:
				requirementPanel = createSkillRequirementPanel((SkillRequirement) requirement);
				break;
			case ABSTRACT:
			default:
				requirementPanel = createAbstractRequirementPanel((AbstractRequirement) requirement);
		}

		wrapper.add(nameLabel, BorderLayout.NORTH);
		wrapper.add(requirementPanel, BorderLayout.CENTER);

		return wrapper;
	}

	private JPanel createSkillRequirementPanel(final SkillRequirement requirement)
	{
		final JPanel wrapper = new JPanel(new GridLayout(1, 3));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JComboBox<Skill> skillComboBox = new JComboBox<>(Skill.values());
		skillComboBox.setForeground(Color.WHITE);
		skillComboBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		skillComboBox.setSelectedIndex(requirement.getSkill().ordinal());
		skillComboBox.addActionListener(e -> {
			requirement.setSkill((Skill) skillComboBox.getSelectedItem());
			refresh();
		});

		final JComboBox<SkillTargetType> targetTypeComboBox = new JComboBox<>(SkillTargetType.values());
		skillComboBox.setForeground(Color.WHITE);
		targetTypeComboBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		targetTypeComboBox.setSelectedIndex(requirement.getTargetType().ordinal());
		targetTypeComboBox.addActionListener(e -> {
			requirement.setTargetType((SkillTargetType) targetTypeComboBox.getSelectedItem());
			refresh();
		});

		final JSpinner valueSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
		valueSpinner.setBackground(ColorScheme.DARK_GRAY_COLOR);
		valueSpinner.setValue(requirement.getTarget());
		valueSpinner.addChangeListener(e -> {
			requirement.setTarget((int) valueSpinner.getValue());
			refresh();
		});

		wrapper.add(valueSpinner);
		wrapper.add(skillComboBox);
		wrapper.add(targetTypeComboBox);

		return wrapper;
	}

	private JPanel createAbstractRequirementPanel(final AbstractRequirement requirement)
	{
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final FlatTextField nameInput = new FlatTextField();
		nameInput.setText(requirement.getName());
		nameInput.setBackground(ColorScheme.DARK_GRAY_COLOR);
		nameInput.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		nameInput.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e)
			{
				requirement.setName(nameInput.getText());
			}
		});

		wrapper.add(nameInput, BorderLayout.CENTER);

		return wrapper;
	}

	private JPanel createAddRequirementPanel()
	{
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JComboBox<RequirementType> dropdown = new JComboBox<>(RequirementType.values());
		dropdown.setBackground(ColorScheme.DARK_GRAY_COLOR);
		dropdown.setSelectedIndex(dropdownIndex);
		dropdown.setRenderer(new RequirementTypeComboBoxRenderer());
		dropdown.addActionListener(e -> {
			dropdownIndex = dropdown.getSelectedIndex();
			refresh();
		});

		final JButton submitButton = new JButton("Add Requirement", ADD_ICON);
		submitButton.setEnabled(dropdownIndex != 0);
		submitButton.setToolTipText("Add Requirement");
		submitButton.addActionListener(e -> {
			if (dropdown.getSelectedItem() != null)
			{
				Requirement requirement = plugin.createRequirement((RequirementType) dropdown.getSelectedItem());

				// Requirement does not need to be registered with the EventBus yet
				dummy.addRequirement(requirement);
			}

			refresh();
		});

		wrapper.add(submitButton, BorderLayout.CENTER);
		wrapper.add(dropdown, BorderLayout.SOUTH);

		return wrapper;
	}

	private JPanel createConfirmationPanel()
	{
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(BorderFactory.createEmptyBorder(20, 14, 0, 14));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JButton confirmButton = new JButton("Confirm");
		final JButton cancelButton = new JButton("Cancel");

		confirmButton.setToolTipText("Confirm");
		confirmButton.addActionListener(e -> updateTarget());

		cancelButton.setToolTipText("Cancel");
		cancelButton.addActionListener(e -> notifyListeners(ACTION_CANCEL));

		wrapper.add(confirmButton, BorderLayout.WEST);
		wrapper.add(cancelButton, BorderLayout.EAST);

		return wrapper;
	}

	private static class RequirementTypeComboBoxRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			JComponent component = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			component.setForeground(index == 0 ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.BRAND_ORANGE);

			if (value instanceof RequirementType)
			{
				RequirementType type = (RequirementType) value;
				component.setToolTipText(type.getDescription());
			}

			return component;
		}
	}
}
