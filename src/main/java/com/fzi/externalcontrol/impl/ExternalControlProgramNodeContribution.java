//-- BEGIN LICENSE BLOCK ----------------------------------------------
// Copyright 2019 FZI Forschungszentrum Informatik
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//-- END LICENSE BLOCK ------------------------------------------------

//----------------------------------------------------------------------
/*!\file
*
* \author  Lea Steffen steffen@fzi.de
* \date    2019-04-18
*
*/
//----------------------------------------------------------------------

package com.fzi.externalcontrol.impl;

import com.ur.urcap.api.contribution.ProgramNodeContribution;
import com.ur.urcap.api.contribution.program.ProgramAPIProvider;
import com.ur.urcap.api.domain.ProgramAPI;
import com.ur.urcap.api.domain.data.DataModel;
import com.ur.urcap.api.domain.undoredo.UndoRedoManager;
import com.ur.urcap.api.domain.undoredo.UndoableChanges;
import com.ur.urcap.api.domain.script.ScriptWriter;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardInputCallback;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardInputFactory;
import com.ur.urcap.api.domain.userinteraction.keyboard.KeyboardTextInput;

public class ExternalControlProgramNodeContribution implements ProgramNodeContribution {
  private static final String ADVANCED_PARAM_KEY = "showadvancedparam";
  private static final boolean ADVANCED_PARAM_DEFAULT = false;
  private static final String MAX_LOST_PACKAGES = "maxlostpackages";
  private static final String MAX_LOST_PACKAGES_DEFAULT_VALUE = "1000";
  private static final String GAIN_SERVO_J = "gain_servo_j";
  private static final String GAIN_SERVO_J_DEFAULT_VALUE = "0";
  private final ProgramAPI programAPI;
  private final DataModel model;
  private final ExternalControlProgramNodeView view;
  private final KeyboardInputFactory keyboardFactory;
  private final UndoRedoManager undoRedoManager;

  private static final String PITASC_APP = "pitascapp";
  private static final String PITASC_DEFAULT_APP = "<undefined>";

  private static final String PITASC_PARAMS = "pitascparams";
  private static final String PITASC_DEFAULT_PARAMS = "";
  
  public ExternalControlProgramNodeContribution(
      ProgramAPIProvider apiProvider, ExternalControlProgramNodeView view, DataModel model) {
    this.programAPI = apiProvider.getProgramAPI();
    this.undoRedoManager = apiProvider.getProgramAPI().getUndoRedoManager();
    this.keyboardFactory =
        apiProvider.getUserInterfaceAPI().getUserInteraction().getKeyboardInputFactory();
    this.model = model;
    this.view = view;
  }

  @Override
  public void openView() {
    view.updateInfoLabel(getInstallation().getHostIP(), getInstallation().getCustomPort());
    view.UpdatePitascAppTextField(getPitascApp());
    view.UpdatePitascParamsTextField(getPitascParams());
  }

  @Override
  public void closeView() {}

  @Override
  public String getTitle() {
    return "pitasc: " + getPitascApp();
  }

  @Override
  public boolean isDefined() {
    return !model.get(PITASC_APP, PITASC_DEFAULT_APP).equals(PITASC_DEFAULT_APP);
  }

  @Override
  public void generateScript(ScriptWriter writer) {
    getInstallation().getPitascCaller().appendNodeLines(writer, getPitascApp(), getPitascParams());

    String urScriptProgram = getInstallation().getUrScriptProgram();
    String uniqueFunName = "fun_" + getInstallation().IncrementInstanceCounter() + "()";
    writer.appendLine("def " + uniqueFunName + ":");
    writer.appendRaw(urScriptProgram);
    writer.appendLine("end");
    writer.appendLine(uniqueFunName);

    getInstallation().getPitascCaller().appendNodePostLines(writer);
  }

  private ExternalControlInstallationNodeContribution getInstallation() {
    return programAPI.getInstallationNode(ExternalControlInstallationNodeContribution.class);
  }

  public void setParam(final String key, final String value, final String default_val) {
    undoRedoManager.recordChanges(new UndoableChanges() {
      @Override
      public void executeChanges() {
        if ("".equals(value)) {
          resetToDefaultValue(key, default_val);
        } else {
          model.set(key, value);
        }
      }
    });
  }

  public String getParam(String key, String default_val) {
    return model.get(key, default_val);
  }

  private void resetToDefaultValue(String key, String default_val) {
    model.set(key, default_val);
  }

  private boolean getAdvancedParam() {
    return model.get(ADVANCED_PARAM_KEY, ADVANCED_PARAM_DEFAULT);
  }

  public void setAdvancedParam(final boolean show) {
    updateAdvancedParam(show);
    // UndoRedoManager is necessary in program node but not in installation node
    // (see here:
    // https://plus.universal-robots.com/apidoc/40237/com/ur/urcap/api/domain/undoredo/undoredomanager.html)
    undoRedoManager.recordChanges(new UndoableChanges() {
      @Override
      public void executeChanges() {
        model.set(ADVANCED_PARAM_KEY, show);
      }
    });
  }

  private void updateAdvancedParam(boolean enable) {
    if (enable) {
      view.showAdvancedParameters(true);
    } else {
      view.showAdvancedParameters(false);
    }
  }

  public KeyboardTextInput getInputForMaxLostPackages() {
    KeyboardTextInput keyboardInput = keyboardFactory.createStringKeyboardInput();
    keyboardInput.setInitialValue(getParam(MAX_LOST_PACKAGES, MAX_LOST_PACKAGES_DEFAULT_VALUE));
    return keyboardInput;
  }

  public KeyboardInputCallback<String> getCallbackForMaxLostPackages() {
    return new KeyboardInputCallback<String>() {
      @Override
      public void onOk(String value) {
        setParam(MAX_LOST_PACKAGES, value, MAX_LOST_PACKAGES_DEFAULT_VALUE);
        view.updateMaxLostPackages_TF(value);
      }
    };
  }

  public KeyboardTextInput getInputForGainServoj() {
    KeyboardTextInput keyboardInput = keyboardFactory.createStringKeyboardInput();
    keyboardInput.setInitialValue(getParam(GAIN_SERVO_J, GAIN_SERVO_J_DEFAULT_VALUE));
    return keyboardInput;
  }

  public KeyboardInputCallback<String> getCallbackForGainServoj() {
    return new KeyboardInputCallback<String>() {
      @Override
      public void onOk(String value) {
        setParam(GAIN_SERVO_J, value, GAIN_SERVO_J_DEFAULT_VALUE);
        view.updateGainServoj_TF(value);
      }
    };
  }

  // port helper functions
  public void setPitascApp(String app) {
    if ("".equals(app)) {
      resetToDefaultPitascApp();
    } else {
      model.set(PITASC_APP, app);
    }
  }

  public String getPitascApp() {
    return model.get(PITASC_APP, PITASC_DEFAULT_APP);
  }

  private void resetToDefaultPitascApp() {
    model.set(PITASC_APP, PITASC_DEFAULT_APP);
  }
  
  public KeyboardTextInput getInputForPitascAppTextField() {
    KeyboardTextInput keyboInput = keyboardFactory.createStringKeyboardInput();
    keyboInput.setInitialValue(getPitascApp());
    return keyboInput;
  }

  public KeyboardInputCallback<String> getCallbackForPitascAppTextField() {
    return new KeyboardInputCallback<String>() {
      @Override
      public void onOk(String value) {
        setPitascApp(value);
        view.UpdatePitascAppTextField(value);
      }
    };
  }

  public void setPitascParams(String params) {
    if ("".equals(params)) {
      resetToDefaultPitascParams();
    } else {
      model.set(PITASC_PARAMS, params);
    }
  }

  public String getPitascParams() {
    return model.get(PITASC_PARAMS, PITASC_DEFAULT_PARAMS);
  }

  private void resetToDefaultPitascParams() {
    model.set(PITASC_PARAMS, PITASC_DEFAULT_PARAMS);
  }

  public KeyboardTextInput getInputForPitascParamsTextField() {
    KeyboardTextInput keyboInput = keyboardFactory.createStringKeyboardInput();
    keyboInput.setInitialValue(getPitascParams());
    return keyboInput;
  }

  public KeyboardInputCallback<String> getCallbackForPitascParamsTextField() {
    return new KeyboardInputCallback<String>() {
      @Override
      public void onOk(String value) {
        setPitascParams(value);
        view.UpdatePitascParamsTextField(value);
      }
    };
  }
}
