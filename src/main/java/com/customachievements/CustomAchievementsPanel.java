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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSeparatorUI;

import lombok.extern.slf4j.Slf4j;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

@Slf4j
public class CustomAchievementsPanel extends PluginPanel
{
	public static final int LIST_ENTRY_HEIGHT = 18;
	public static final int LIST_ENTRY_GAP = LIST_ENTRY_HEIGHT + 1;
	public static final int BUTTON_WIDTH = 24;
	public static final int INDENT_WIDTH = 24;

	private static final ImageIcon IMPORT_ICON;
	private static final ImageIcon IMPORT_ICON_HOVER;
	private static final ImageIcon EXPORT_ICON;
	private static final ImageIcon EXPORT_ICON_HOVER;
	private static final ImageIcon ADD_ICON;
	private static final ImageIcon ADD_ICON_HOVER;
	private static final ImageIcon REMOVE_ICON;
	private static final ImageIcon REMOVE_ICON_FADED;
	private static final ImageIcon RESET_ICON;
	private static final ImageIcon RESET_ICON_FADED;
	private static final ImageIcon EDIT_ICON;
	private static final ImageIcon EDIT_ICON_HOVER;
	private static final ImageIcon EDIT_ICON_SELECTED;
	private static final ImageIcon MINI_EDIT_ICON;
	private static final ImageIcon MINI_EDIT_ICON_FADED;
	private static final ImageIcon DRAG_ICON;
	private static final ImageIcon DRAG_ICON_FADED;
	private static final ImageIcon EXPAND_ICON;
	private static final ImageIcon COLLAPSE_ICON;

	private static final String TITLE_MAIN = "Achievements";
	private static final String TITLE_EDIT = "Editor";
	private static final String INFO_USAGE;
	private static final String INFO_EDIT;

	private final JPanel achievementsPanel = new FixedWidthPanel();

	private final JLabel title = new JLabel();
	private final JLabel info = new JLabel();
	private final JSeparator infoSeparator = new JSeparator();

	private final JButton clearButton = new JButton();
	private final JButton importButton = new JButton();
	private final JButton exportButton = new JButton();
	private final JButton addButton = new JButton();
	private final JToggleButton editToggle = new JToggleButton();

	private final List<JSeparator> insertionIndicators = new ArrayList<>();
	private final Map<AchievementElement, Integer> insertionIndicatorIndexMap = new HashMap<>();
	private boolean dragging = false;

	private final CustomAchievementsPlugin plugin;
	private final CustomAchievementsConfig config;

	private final EditAchievementPanel editAchievementPanel;

	static
	{
		final BufferedImage importImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "import_icon.png");
		final BufferedImage exportImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "export_icon.png");
		final BufferedImage addImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "add_icon.png");
		final BufferedImage removeImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "mini_remove_icon.png");
		final BufferedImage resetImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "mini_reset_icon.png");
		final BufferedImage editImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "edit_icon.png");
		final BufferedImage invertedEditImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "edit_icon_inverted.png");
		final BufferedImage miniEditImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "mini_edit_icon.png");
		final BufferedImage expandImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "expand_icon.png");
		final BufferedImage collapseImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "collapse_icon.png");
		final BufferedImage dragImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "drag_icon.png");

		IMPORT_ICON = new ImageIcon(importImage);
		IMPORT_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(importImage, 0.5f));

		EXPORT_ICON = new ImageIcon(exportImage);
		EXPORT_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(exportImage, 0.5f));

		ADD_ICON = new ImageIcon(addImage);
		ADD_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(addImage, 0.5f));

		REMOVE_ICON = new ImageIcon(removeImage);
		REMOVE_ICON_FADED = new ImageIcon(ImageUtil.alphaOffset(removeImage, 0.2f));

		RESET_ICON = new ImageIcon(resetImage);
		RESET_ICON_FADED = new ImageIcon(ImageUtil.alphaOffset(resetImage, 0.2f));

		EDIT_ICON = new ImageIcon(editImage);
		EDIT_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(editImage, 0.5f));
		EDIT_ICON_SELECTED = new ImageIcon(invertedEditImage);

		MINI_EDIT_ICON = new ImageIcon(miniEditImage);
		MINI_EDIT_ICON_FADED = new ImageIcon(ImageUtil.alphaOffset(miniEditImage, 0.2f));

		DRAG_ICON = new ImageIcon(dragImage);
		DRAG_ICON_FADED = new ImageIcon(ImageUtil.alphaOffset(dragImage, 0.2f));

		EXPAND_ICON = new ImageIcon(ImageUtil.luminanceScale(expandImage, 0.4f));
		COLLAPSE_ICON = new ImageIcon(ImageUtil.luminanceScale(collapseImage, 0.4f));

		INFO_USAGE = "<html>"
				+ "Create and edit Custom Achievements using the menu buttons above. Additional tools are shown in "
				+ "Edit Mode. For help and usage visit the custom-achievements GitHub page."
				+ "</html>";

		INFO_EDIT = "<html>"
				+ "NOTE: Progress made toward a Requirement will NOT be overwritten ONLY when editing quantity attributes."
				+ "</html>";
	}

	CustomAchievementsPanel(final CustomAchievementsPlugin plugin, final CustomAchievementsConfig config)
	{
		super(false);

		this.plugin = plugin;
		this.config = config;
		this.editAchievementPanel = new EditAchievementPanel(plugin);

		editAchievementPanel.setVisible(false);
		editAchievementPanel.addActionListener(e -> {
			// An ActionEvent will signal that we are done editing
			editAchievementPanel.setVisible(false);
			refresh();
		});

		final JPanel headerPanel = new JPanel();
		final JPanel actionsWrapper = new JPanel();
		final JPanel infoWrapper = new JPanel();
		final JPanel achievementsWrapper = new JPanel();

		final JScrollPane achievementsScrollPane = new JScrollPane(achievementsWrapper);
		achievementsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		headerPanel.setLayout(new BorderLayout());
		headerPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));

		actionsWrapper.setLayout(new GridLayout(1, 4, -10, 0));
		infoWrapper.setLayout(new BorderLayout());
		achievementsWrapper.setLayout(new BorderLayout());
		achievementsWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		achievementsPanel.setLayout(new GridBagLayout());
		achievementsPanel.setBorder(BorderFactory.createEmptyBorder());

		title.setForeground(Color.WHITE);
		info.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		info.setFont(FontManager.getRunescapeSmallFont());
		info.setBorder(BorderFactory.createEmptyBorder(BORDER_OFFSET, 0, BORDER_OFFSET, 0));

		infoSeparator.setUI(new BasicSeparatorUI());
		infoSeparator.setBackground(ColorScheme.LIGHT_GRAY_COLOR);

		clearButton.setText("Clear All");
		clearButton.setVisible(false);
		clearButton.setBorder(BorderFactory.createEmptyBorder(BORDER_OFFSET, 0, BORDER_OFFSET, 0));
		clearButton.addActionListener(e -> clearAchievementElements());

		SwingUtil.removeButtonDecorations(importButton);
		importButton.setIcon(IMPORT_ICON);
		importButton.setRolloverIcon(IMPORT_ICON_HOVER);
		importButton.setPressedIcon(IMPORT_ICON_HOVER);
		importButton.setToolTipText("Import Achievements File...");
		importButton.addActionListener(e -> importFromFile());

		SwingUtil.removeButtonDecorations(exportButton);
		exportButton.setIcon(EXPORT_ICON);
		exportButton.setRolloverIcon(EXPORT_ICON_HOVER);
		exportButton.setPressedIcon(EXPORT_ICON_HOVER);
		exportButton.setToolTipText("Export Achievements File...");
		exportButton.addActionListener(e -> exportToFile());

		SwingUtil.removeButtonDecorations(addButton);
		addButton.setIcon(ADD_ICON);
		addButton.setRolloverIcon(ADD_ICON_HOVER);
		addButton.setPressedIcon(ADD_ICON_HOVER);
		addButton.setToolTipText("Add New Achievement");
		addButton.addActionListener(e -> {
			editAchievementPanel.setVisible(true);
			editAchievementPanel.setTarget(plugin.getElements().size(), null, plugin.createAchievement("New Achievement"));
			refresh();
		});

		SwingUtil.removeButtonDecorations(editToggle);
		editToggle.setIcon(EDIT_ICON);
		editToggle.setRolloverIcon(EDIT_ICON_HOVER);
		editToggle.setSelectedIcon(EDIT_ICON_SELECTED);
		editToggle.setToolTipText("Toggle Edit Mode");
		editToggle.addActionListener(e -> refresh());

		actionsWrapper.add(importButton);
		actionsWrapper.add(exportButton);
		actionsWrapper.add(addButton);
		actionsWrapper.add(editToggle);
		infoWrapper.add(infoSeparator, BorderLayout.NORTH);
		infoWrapper.add(info, BorderLayout.SOUTH);
		achievementsWrapper.add(achievementsPanel, BorderLayout.NORTH);

		headerPanel.add(title, BorderLayout.WEST);
		headerPanel.add(infoWrapper, BorderLayout.SOUTH);
		headerPanel.add(actionsWrapper, BorderLayout.EAST);

		add(headerPanel, BorderLayout.NORTH);
		add(achievementsScrollPane, BorderLayout.CENTER);
		add(clearButton, BorderLayout.SOUTH);

		refresh();
	}

	public void importFromFile()
	{
		JFileChooser fc = new JFileChooser();

		fc.setDialogType(JFileChooser.OPEN_DIALOG);
		fc.setDialogTitle("Choose a Custom Achievements JSON file to import");
		fc.setFileFilter(new FileNameExtensionFilter("JSON", "json", "JSON"));

		if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		File file = fc.getSelectedFile();

		if (file == null)
		{
			return;
		}

		if (!plugin.getElements().isEmpty())
		{
			int confirm = JOptionPane.showConfirmDialog(this,
					"Are you sure you want to import this file? This action will DELETE all current achievements.",
					"Warning",
					JOptionPane.OK_CANCEL_OPTION);

			if (confirm != JOptionPane.OK_OPTION)
			{
				return;
			}
		}

		try (BufferedReader in = new BufferedReader(new FileReader(file)))
		{
			StringBuilder json = new StringBuilder();
			in.lines().forEachOrdered(line -> json.append(line).append(System.lineSeparator()));

			plugin.loadConfig(json.toString());
			plugin.updateConfig();
		}
		catch (FileNotFoundException e)
		{
			JOptionPane.showConfirmDialog(this,
					"File does not exist.",
					"Error",
					JOptionPane.DEFAULT_OPTION);
		}
		catch (IOException e)
		{
			JOptionPane.showConfirmDialog(this,
					"An error occurred while attempting to import file.",
					"Error",
					JOptionPane.DEFAULT_OPTION);
		}
	}

	public void exportToFile()
	{
		JFileChooser fc = new JFileChooser();

		fc.setDialogType(JFileChooser.SAVE_DIALOG);
		fc.setDialogTitle("Export Custom Achievements to a JSON file");
		fc.setSelectedFile(new File("achievements.json"));
		fc.setFileFilter(new FileNameExtensionFilter("JSON", "json", "JSON"));

		if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		File file = fc.getSelectedFile();

		if (file == null)
		{
			return;
		}
		else if (!file.getName().toLowerCase().endsWith(".json"))
		{
			file = new File(file.getParentFile(), file.getName() + ".json");
		}

		try (FileWriter out = new FileWriter(file))
		{
			plugin.updateConfig();
			out.write(config.achievementsData());
		}
		catch (IOException e)
		{
			JOptionPane.showConfirmDialog(this,
					"An error occurred while attempting to write to file.",
					"Error",
					JOptionPane.DEFAULT_OPTION);
		}
	}

	public void refresh()
	{
		achievementsPanel.removeAll();
		insertionIndicators.clear();
		insertionIndicatorIndexMap.clear();

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.gridx = 0;
		gbc.gridy = 0;

		final boolean infoHeaderVisible = config.isInfoHeaderVisible();
		info.setVisible(infoHeaderVisible);
		infoSeparator.setVisible(infoHeaderVisible);

		if (editAchievementPanel.isVisible())
		{
			// Disable actions while editing achievements
			enableActions(false);

			title.setText(TITLE_EDIT);
			info.setText(INFO_EDIT);
			clearButton.setVisible(false);

			achievementsPanel.add(editAchievementPanel, gbc);
		}
		else
		{
			final Deque<Deque<AchievementElement>> stack = new ArrayDeque<>();
			final Deque<AchievementElement> parents = new ArrayDeque<>();
			Deque<AchievementElement> elements;

			JPanel wrapper;
			ActionListener expandCallback;
			ActionListener editCallback;
			ActionListener resetCallback;
			ActionListener removeCallback;
			DragAdapter<AchievementElement> dragAdapter;

			JSeparator indicator;

			// Re-enable actions
			enableActions(true);

			title.setText(TITLE_MAIN);
			info.setText(INFO_USAGE);
			clearButton.setVisible(editToggle.isSelected());

			stack.push(new ArrayDeque<>(plugin.getElements()));

			while (!stack.isEmpty())
			{
				elements = stack.peek();

				while (!elements.isEmpty())
				{
					final AchievementElement parent = parents.peek();
					final AchievementElement element = elements.pop();
					final List<AchievementElement> elementsRef = parent == null ?
							plugin.getElements() :
							parent.getChildren();
					final int index = elementsRef.size() - elements.size() - 1;

					element.refresh();
					JLabel label = createAchievementElement(element);

					expandCallback = e -> {
						element.setUiExpanded(!element.isUiExpanded());
						refresh();
					};

					editCallback = e -> {
						editAchievementPanel.setVisible(true);
						editAchievementPanel.setTarget(index, parent, element);
						refresh();
					};

					resetCallback = e -> {
						element.reset();
						plugin.updateConfig();
						refresh();
					};

					if (parent == null)
					{
						removeCallback = e -> {
							plugin.remove(element);
							plugin.updateConfig();
							refresh();
						};
					}
					else
					{
						removeCallback = e -> {
							plugin.remove(parent, element);
							plugin.updateConfig();
							refresh();
						};
					}

					dragAdapter = new DragAdapter<AchievementElement>(index, elementsRef)
					{
						@Override
						public int indicatorIndex(int listIndex)
						{
							if (listIndex < elementsRef.size())
							{
								return insertionIndicatorIndexMap.get(elementsRef.get(listIndex));
							}
							else
							{
								return insertionIndicatorIndexMap.get(elementsRef.get(elementsRef.size() - 1)) + 1;
							}
						}
					};

					if (element.getChildren().isEmpty())
					{
						wrapper = createElementWrapper(
								label,
								editCallback,
								resetCallback,
								removeCallback,
								dragAdapter,
								stack.size()
						);
					}
					else
					{
						wrapper = createExpandableElementWrapper(
								label,
								expandCallback,
								editCallback,
								resetCallback,
								removeCallback,
								dragAdapter,
								stack.size(),
								element.isUiExpanded()
						);
					}

					dragAdapter.setHighlightComponent(wrapper);

					indicator = createInsertionIndicator();
					insertionIndicatorIndexMap.put(element, insertionIndicators.size() - 1);
					achievementsPanel.add(indicator, gbc);
					gbc.gridy++;

					achievementsPanel.add(wrapper, gbc);
					gbc.gridy++;

					if (element.isUiExpanded() && !element.getChildren().isEmpty())
					{
						stack.push(new ArrayDeque<>(element.getChildren()));
						parents.push(element);
						break;
					}
				}

				if (stack.peek().isEmpty())
				{
					stack.pop();

					if (!parents.isEmpty())
					{
						parents.pop();
					}
				}
			}

			indicator = createInsertionIndicator();
			achievementsPanel.add(indicator, gbc);
		}

		revalidate();
		repaint();
	}

	private void enableActions(boolean enable)
	{
		importButton.setEnabled(enable);
		exportButton.setEnabled(enable);
		addButton.setEnabled(enable);
		editToggle.setEnabled(enable);
	}

	private void clearAchievementElements()
	{
		int confirm = JOptionPane.showConfirmDialog(this,
				"Are you sure you want to DELETE all current achievement data?",
				"Warning",
				JOptionPane.OK_CANCEL_OPTION);

		if (confirm == JOptionPane.OK_OPTION)
		{
			plugin.clear();
			plugin.updateConfig();
			refresh();
		}
	}

	private JLabel createAchievementElement(AchievementElement element)
	{
		String forceIndicator = element.isForceComplete() ? " *" : "";

		JLabel label = new JLabel();
		label.setForeground(element.getState().getColor());
		label.setText(String.format("%s%s", element.toString(), forceIndicator));
		label.setToolTipText(String.format("%s%s: %s", element.toString(), forceIndicator, element.getState().toString()));
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				element.click();
				element.refresh();

				plugin.updateConfig();
				refresh();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!dragging)
				{
					label.setForeground(Color.WHITE);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				label.setForeground(element.getState().getColor());
			}
		});

		return label;
	}

	private JSeparator createInsertionIndicator()
	{
		final JSeparator indicator = new JSeparator();

		indicator.setUI(new BasicSeparatorUI());
		indicator.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		insertionIndicators.add(indicator);

		return indicator;
	}

	private <T> JPanel createExpandableElementWrapper(
			JLabel label,
			ActionListener expandCallback,
			ActionListener editCallback,
			ActionListener resetCallback,
			ActionListener removeCallback,
			DragAdapter<T> dragAdapter,
			int indent,
			boolean expanded)
	{
		final JPanel wrapper = createElementWrapper(
				label,
				editCallback,
				resetCallback,
				removeCallback,
				dragAdapter,
				indent - 1);

		final JButton expandButton = new JButton(expanded ? COLLAPSE_ICON : EXPAND_ICON);
		SwingUtil.removeButtonDecorations(expandButton);
		expandButton.setPreferredSize(new Dimension(BUTTON_WIDTH, wrapper.getHeight()));
		expandButton.setToolTipText(expanded ? "Collapse" : "Expand");
		expandButton.addActionListener(expandCallback);

		wrapper.add(expandButton, BorderLayout.WEST);

		return wrapper;
	}

	private <T> JPanel createElementWrapper(
			JLabel label,
			ActionListener editCallback,
			ActionListener resetCallback,
			ActionListener removeCallback,
			DragAdapter<T> dragAdapter,
			int indent)
	{
		final JPanel wrapper = new FixedWidthPanel();
		wrapper.setLayout(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		wrapper.setBorder(BorderFactory.createEmptyBorder(0, Math.max(0, indent * INDENT_WIDTH), 2, 0));
		wrapper.setPreferredSize(new Dimension(0, LIST_ENTRY_HEIGHT));

		label.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));

		if (editToggle.isSelected())
		{
			JPanel editWrapper = new JPanel(new GridLayout(1, 4, -6, 0));
			editWrapper.setOpaque(false);

			JButton dragButton = new JButton(DRAG_ICON_FADED);
			SwingUtil.removeButtonDecorations(dragButton);
			dragButton.setPreferredSize(new Dimension(BUTTON_WIDTH, LIST_ENTRY_HEIGHT));
			dragButton.setRolloverIcon(DRAG_ICON);
			dragButton.setToolTipText("Drag");
			dragButton.addMouseListener(dragAdapter);
			dragButton.addMouseMotionListener(dragAdapter);

			JButton editButton = new JButton(MINI_EDIT_ICON_FADED);
			SwingUtil.removeButtonDecorations(editButton);
			editButton.setPreferredSize(new Dimension(BUTTON_WIDTH, LIST_ENTRY_HEIGHT));
			editButton.setRolloverIcon(MINI_EDIT_ICON);
			editButton.setToolTipText("Edit");
			editButton.addActionListener(editCallback);

			JButton resetButton = new JButton(RESET_ICON_FADED);
			SwingUtil.removeButtonDecorations(resetButton);
			resetButton.setPreferredSize(new Dimension(BUTTON_WIDTH, LIST_ENTRY_HEIGHT));
			resetButton.setRolloverIcon(RESET_ICON);
			resetButton.setToolTipText("Reset Progress");
			resetButton.addActionListener(resetCallback);

			JButton removeButton = new JButton(REMOVE_ICON_FADED);
			SwingUtil.removeButtonDecorations(removeButton);
			removeButton.setPreferredSize(new Dimension(BUTTON_WIDTH, LIST_ENTRY_HEIGHT));
			removeButton.setRolloverIcon(REMOVE_ICON);
			removeButton.setToolTipText("Remove");
			removeButton.addActionListener(removeCallback);

			editWrapper.add(dragButton);
			editWrapper.add(editButton);
			editWrapper.add(resetButton);
			editWrapper.add(removeButton);
			wrapper.add(editWrapper, BorderLayout.EAST);
		}

		wrapper.add(label, BorderLayout.CENTER);

		return wrapper;
	}

	private abstract class DragAdapter<T> extends MouseAdapter
	{
		private static final int HALF_LIST_ENTRY_GAP = LIST_ENTRY_GAP / 2;

		private final int index;
		private final List<T> list;
		private JComponent component;

		private int selectedIndex;
		private JSeparator indicator;

		public DragAdapter(int index, List<T> list)
		{
			this(index, list, null);
		}

		public DragAdapter(int index, List<T> list, JComponent component)
		{
			this.index = index;
			this.list = list;
			this.component = component;
			this.selectedIndex = index;
			this.indicator = null;
		}

		/**
		 * Returns the insertion indicator index given list index. Must handle values between 0 and list.size().
		 */
		public abstract int indicatorIndex(int listIndex);

		public void setHighlightComponent(JComponent component)
		{
			this.component = component;
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			selectedIndex = index;
			indicator = insertionIndicators.get(indicatorIndex(index));

			if (component != null)
			{
				component.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			dragging = true;
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			if (selectedIndex > index)
			{
				// Directional offset
				selectedIndex -= 1;
			}

			if (selectedIndex != index)
			{
				list.add(selectedIndex, list.remove(index));
			}

			dragging = false;
			plugin.updateConfig();
			refresh();
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{
			int screenY = e.getYOnScreen();
			int indicatorY = indicator.getLocationOnScreen().y;

			JSeparator nextIndicator;
			int nextIndex;
			int distance;

			if (screenY - indicatorY > 0)
			{
				nextIndex = Math.min(list.size(), selectedIndex + 1);
				nextIndicator = insertionIndicators.get(indicatorIndex(nextIndex));
				distance = nextIndicator.getLocationOnScreen().y - screenY;
			}
			else
			{
				nextIndex = Math.max(0, selectedIndex - 1);
				nextIndicator = insertionIndicators.get(indicatorIndex(nextIndex));
				distance = screenY - nextIndicator.getLocationOnScreen().y;
			}

			if (distance <= HALF_LIST_ENTRY_GAP)
			{
				selectedIndex = nextIndex;
				setIndicator(nextIndicator);
			}
		}

		private void setIndicator(JSeparator separator)
		{
			indicator.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			indicator = separator;
			indicator.setBackground(Color.RED);
		}
	}
}
