package io.unthrottled.amii.config.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import io.unthrottled.amii.assets.CharacterEntity;
import io.unthrottled.amii.assets.Gender;
import io.unthrottled.amii.config.Config;
import io.unthrottled.amii.config.ConfigListener;
import io.unthrottled.amii.config.ConfigSettingsModel;
import io.unthrottled.amii.config.PluginSettings;
import io.unthrottled.amii.memes.PanelDismissalOptions;
import io.unthrottled.amii.tools.PluginMessageBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.unthrottled.amii.events.UserEvents.IDLE;
import static io.unthrottled.amii.events.UserEvents.LOGS;
import static io.unthrottled.amii.events.UserEvents.PROCESS;
import static io.unthrottled.amii.events.UserEvents.STARTUP;
import static io.unthrottled.amii.events.UserEvents.TASK;
import static io.unthrottled.amii.events.UserEvents.TEST;
import static io.unthrottled.amii.memes.PanelDismissalOptions.FOCUS_LOSS;
import static io.unthrottled.amii.memes.PanelDismissalOptions.TIMED;
import static java.util.Optional.ofNullable;

public class PluginSettingsUI implements SearchableConfigurable, Configurable.NoScroll, DumbAware {

  private final ConfigSettingsModel initialSettings = PluginSettings.getInitialConfigSettingsModel();
  private final ConfigSettingsModel pluginSettingsModel = PluginSettings.getInitialConfigSettingsModel();
  private JPanel rootPanel;
  private JTabbedPane optionsPane;
  private JRadioButton timedDismissRadioButton;
  private JRadioButton focusLossRadioButton;
  private JPanel anchorPanel;
  private JSpinner timedMemeDurationSpinner;
  private JSpinner invulnerablilityDurationSpinner;
  private JCheckBox soundEnabled;
  private JSlider volumeSlider;
  private JSpinner eventsBeforeFrustrationSpinner;
  private JSlider frustrationProbabilitySlider;
  private JCheckBox allowFrustrationCheckBox;
  private JCheckBox startupEnabled;
  private JCheckBox buildResultsEnabled;
  private JCheckBox testResultsEnabled;
  private JCheckBox idleEnabled;
  private JSpinner idleTimeoutSpinner;
  private JPanel exitCodePanel;
  private JCheckBox exitCodeEnabled;
  private JCheckBox preferFemale;
  private JCheckBox preferMale;
  private JCheckBox preferOther;
  private JPanel preferredCharacters;
  private JCheckBox watchLogs;
  private JTextField logKeyword;
  private JCheckBox ignoreCase;
  private PreferredCharacterPanel characterModel;
  private JBTable exitCodeTable;
  private ListTableModel<Integer> exitCodeListModel;

  private void createUIComponents() {
    anchorPanel = AnchorPanelFactory.getAnchorPositionPanel(
      Config.getInstance().getNotificationAnchor(), notificationAnchor ->
        pluginSettingsModel.setMemeDisplayAnchorValue(notificationAnchor.name())
    );

    characterModel = new PreferredCharacterPanel();
    preferredCharacters = characterModel.getComponent();
    preferredCharacters.setPreferredSize(JBUI.size(800, 600));
    preferredCharacters.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    exitCodeListModel = new ListTableModel<Integer>() {
      @Override
      public void addRow() {
        addRow(0);
      }
    };
    exitCodeListModel.addTableModelListener(e -> ofNullable(pluginSettingsModel) // Does not get instantiated
      .ifPresent(settings -> settings.setAllowedExitCodes(getSelectedExitCodes()))); // may be because of bytecode
    exitCodeTable = new JBTable(exitCodeListModel);                                  // instrumentation /shrug/
    exitCodeListModel.setColumnInfos(new ColumnInfo[]{new ColumnInfo<Integer, String>("Exit Code") {

      @Override
      public String valueOf(Integer exitCode) {
        return exitCode.toString();
      }

      @Override
      public void setValue(Integer s, String value) {
        int currentRowIndex = exitCodeTable.getSelectedRow();
        if (StringUtil.isEmpty(value) && currentRowIndex >= 0 &&
          currentRowIndex < exitCodeListModel.getRowCount()) {
          exitCodeListModel.removeRow(currentRowIndex);
        } else {
          exitCodeListModel.insertRow(currentRowIndex, Integer.parseInt(value));
          exitCodeListModel.removeRow(currentRowIndex + 1);
          exitCodeTable.transferFocus();
        }
      }

      @Override
      public boolean isCellEditable(Integer info) {
        return true;
      }
    }});
    exitCodeTable.getColumnModel().setColumnMargin(0);
    exitCodeTable.setShowColumns(false);
    exitCodeTable.setShowGrid(false);

    exitCodeTable.getEmptyText().setText(PluginMessageBundle.message("settings.exit.code.no.codes"));

    exitCodeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    exitCodePanel = ToolbarDecorator.createDecorator(exitCodeTable)
      .disableUpDownActions().createPanel();
  }

  @Override
  public @NotNull String getId() {
    return "io.unthrottled.amii.config.PluginSettings";
  }

  @Override
  public @NlsContexts.ConfigurableName String getDisplayName() {
    return "AMII Settings";
  }

  @Override
  public @Nullable JComponent createComponent() {
    Config config = Config.getInstance();

    PanelDismissalOptions notificationMode = config.getNotificationMode();
    timedDismissRadioButton.setSelected(TIMED.equals(notificationMode));
    focusLossRadioButton.setSelected(FOCUS_LOSS.equals(notificationMode));
    enableCorrectSpinner(notificationMode);
    ActionListener dismissalListener = a -> {
      PanelDismissalOptions memeDisplayModeValue = timedDismissRadioButton.isSelected() ?
        TIMED :
        FOCUS_LOSS;
      enableCorrectSpinner(memeDisplayModeValue);
      pluginSettingsModel.setMemeDisplayModeValue(
        memeDisplayModeValue.name()
      );
    };
    timedDismissRadioButton.addActionListener(dismissalListener);
    focusLossRadioButton.addActionListener(dismissalListener);

    SpinnerNumberModel timedMemeDurationModel = new SpinnerNumberModel(
      config.getMemeDisplayTimedDuration(),
      10,
      Integer.MAX_VALUE,
      1
    );
    timedMemeDurationSpinner.setModel(timedMemeDurationModel);
    timedMemeDurationSpinner.addChangeListener(change ->
      pluginSettingsModel.setMemeDisplayTimedDuration(
        timedMemeDurationModel.getNumber().intValue()
      ));

    SpinnerNumberModel invulnerabilityDurationModel = new SpinnerNumberModel(
      config.getMemeDisplayInvulnerabilityDuration(),
      0,
      Integer.MAX_VALUE,
      1
    );
    invulnerablilityDurationSpinner.setModel(invulnerabilityDurationModel);
    invulnerablilityDurationSpinner.addChangeListener(change ->
      pluginSettingsModel.setMemeDisplayInvulnerabilityDuration(
        invulnerabilityDurationModel.getNumber().intValue()
      ));

    allowFrustrationCheckBox.addActionListener(e -> {
      updateFrustrationComponents();
      pluginSettingsModel.setAllowFrustration(allowFrustrationCheckBox.isSelected());
    });
    frustrationProbabilitySlider.setForeground(UIUtil.getContextHelpForeground());

    soundEnabled.addActionListener(e -> volumeSlider.setEnabled(soundEnabled.isSelected()));
    volumeSlider.setForeground(UIUtil.getContextHelpForeground());
    volumeSlider.addChangeListener(
      e -> pluginSettingsModel.setMemeVolume(((JSlider) e.getSource()).getModel().getValue())
    );

    preferFemale.addActionListener(e -> updateGenderPreference(Gender.FEMALE.getValue(), preferFemale.isSelected()));
    preferMale.addActionListener(e -> updateGenderPreference(Gender.MALE.getValue(), preferMale.isSelected()));
    preferOther.addActionListener(e -> updateGenderPreference(Gender.OTHER.getValue(), preferOther.isSelected()));

    idleEnabled.addActionListener(e -> {
      updateIdleComponents();
      updateEventPreference(IDLE.getValue(), idleEnabled.isSelected());
    });
    watchLogs.addActionListener(e -> {
      updateLogComponents();
      updateEventPreference(LOGS.getValue(), watchLogs.isSelected());
    });
    exitCodeEnabled.addActionListener(e -> {
      updateExitCodeComponents();
      updateEventPreference(PROCESS.getValue(), exitCodeEnabled.isSelected());
    });
    startupEnabled.addActionListener(e -> updateEventPreference(STARTUP.getValue(), startupEnabled.isSelected()));
    buildResultsEnabled.addActionListener(e -> updateEventPreference(TASK.getValue(), buildResultsEnabled.isSelected()));
    testResultsEnabled.addActionListener(e -> updateEventPreference(TEST.getValue(), testResultsEnabled.isSelected()));


    logKeyword.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        updateText();
      }

      private void updateText() {
        pluginSettingsModel.setLogKeyword(logKeyword.getText());
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        updateText();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        updateText();
      }
    });
    ignoreCase.addActionListener(e -> pluginSettingsModel.setLogSearchIgnoreCase(ignoreCase.isSelected()));

    initFromState();
    return rootPanel;
  }

  private void updateExitCodeComponents() {
    exitCodePanel.setEnabled(exitCodeEnabled.isSelected());
    exitCodeTable.setEnabled(exitCodeEnabled.isSelected());
  }

  private void updateLogComponents() {
    ignoreCase.setEnabled(watchLogs.isSelected());
    logKeyword.setEnabled(watchLogs.isSelected());
  }

  private void updateIdleComponents() {
    idleTimeoutSpinner.setEnabled(idleEnabled.isSelected());
  }

  private void updateFrustrationComponents() {
    frustrationProbabilitySlider.setEnabled(allowFrustrationCheckBox.isSelected());
    eventsBeforeFrustrationSpinner.setEnabled(allowFrustrationCheckBox.isSelected());
  }

  private void updateGenderPreference(int value, boolean selected) {
    int preferredGenders = pluginSettingsModel.getPreferredGenders();
    pluginSettingsModel.setPreferredGenders(
      selected ?
        preferredGenders | value :
        preferredGenders ^ value
    );
  }

  private void updateEventPreference(int eventCode, boolean selected) {
    int enabledEvents = pluginSettingsModel.getEnabledEvents();
    pluginSettingsModel.setEnabledEvents(
      selected ?
        enabledEvents | eventCode :
        enabledEvents ^ eventCode
    );
  }

  private void initFromState() {
    allowFrustrationCheckBox.setSelected(initialSettings.getAllowFrustration());
    updateFrustrationComponents();
    soundEnabled.setSelected(initialSettings.getSoundEnabled());
    volumeSlider.getModel().setValue(initialSettings.getMemeVolume());
    preferFemale.setSelected(isGenderSelected(Gender.FEMALE.getValue()));
    preferMale.setSelected(isGenderSelected(Gender.MALE.getValue()));
    preferOther.setSelected(isGenderSelected(Gender.OTHER.getValue()));

    idleEnabled.setSelected(isEventEnabled(IDLE.getValue()));
    updateIdleComponents();
    watchLogs.setSelected(isEventEnabled(LOGS.getValue()));
    updateLogComponents();
    exitCodeEnabled.setSelected(isEventEnabled(PROCESS.getValue()));
    updateExitCodeComponents();
    startupEnabled.setSelected(isEventEnabled(STARTUP.getValue()));
    buildResultsEnabled.setSelected(isEventEnabled(TASK.getValue()));
    testResultsEnabled.setSelected(isEventEnabled(TEST.getValue()));
    initializeExitCodes();
    logKeyword.setText(initialSettings.getLogKeyword());
    ignoreCase.setSelected(initialSettings.getLogSearchIgnoreCase());
  }

  private void initializeExitCodes() {
    int preExistingRows = exitCodeListModel.getRowCount();
    if (preExistingRows > 0) {
      IntStream.range(0, preExistingRows)
        .forEach(idx -> exitCodeListModel.removeRow(0));
    }
    Arrays.stream(pluginSettingsModel.getAllowedExitCodes()
      .split(Config.DEFAULT_DELIMITER))
      .filter(code -> !StringUtil.isEmpty(code))
      .map(Integer::parseInt)
      .forEach(exitCodeListModel::addRow);
  }

  private boolean isGenderSelected(int genderCode) {
    return (initialSettings.getPreferredGenders() & genderCode) == genderCode;
  }

  private boolean isEventEnabled(int eventCode) {
    return (initialSettings.getEnabledEvents() & eventCode) == eventCode;
  }

  private void enableCorrectSpinner(PanelDismissalOptions memeDisplayModeValue) {
    invulnerablilityDurationSpinner.setEnabled(memeDisplayModeValue == FOCUS_LOSS);
    timedMemeDurationSpinner.setEnabled(memeDisplayModeValue == TIMED);
  }

  @Override
  public boolean isModified() {
    return !initialSettings.equals(pluginSettingsModel) ||
      characterModel.isModified();
  }

  @Override
  public void apply() {
    Config config = Config.getInstance();
    config.setMemeDisplayAnchorValue(pluginSettingsModel.getMemeDisplayAnchorValue());
    config.setMemeDisplayModeValue(pluginSettingsModel.getMemeDisplayModeValue());
    config.setMemeDisplayInvulnerabilityDuration(pluginSettingsModel.getMemeDisplayInvulnerabilityDuration());
    config.setMemeDisplayTimedDuration(pluginSettingsModel.getMemeDisplayTimedDuration());
    config.setPreferredCharacters(
      characterModel.getSelected().stream()
        .map(CharacterEntity::getId)
        .collect(Collectors.joining(Config.DEFAULT_DELIMITER))
    );
    config.setSoundEnabled(pluginSettingsModel.getSoundEnabled());
    config.setMemeVolume(pluginSettingsModel.getMemeVolume());
    config.setPreferredGenders(pluginSettingsModel.getPreferredGenders());
    config.setAllowFrustration(pluginSettingsModel.getAllowFrustration());
    config.setEnabledEvents(pluginSettingsModel.getEnabledEvents());
    config.setAllowedExitCodes(getSelectedExitCodes());
    config.setLogSearchTerms(pluginSettingsModel.getLogKeyword());
    config.setLogSearchIgnoreCase(pluginSettingsModel.getLogSearchIgnoreCase());
    ApplicationManager.getApplication().getMessageBus().syncPublisher(
      ConfigListener.Companion.getCONFIG_TOPIC()
    ).pluginConfigUpdated(config);
  }

  @NotNull
  private String getSelectedExitCodes() {
    return IntStream.range(0, exitCodeListModel.getRowCount())
      .map(exitCodeListModel::getRowValue)
      .distinct()
      .sorted()
      .mapToObj(String::valueOf)
      .collect(Collectors.joining(Config.DEFAULT_DELIMITER));
  }
}
