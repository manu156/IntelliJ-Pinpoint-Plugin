package com.github.manu156.pinpointintegration.window;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(
        name = "com.github.manu156.BasicConfig",
        storages = @Storage(value = "com.github.manu156.basicConfig.xml", roamingType = RoamingType.PER_OS)
)
public class BasicConfig implements PersistentStateComponent<BasicConfig.State> {

    static class State {
        public String url;
        public String server;
        public String xGroupUnnit;
        public String yGroupUnit;
        public String limit;
    }

    private State myState = new State();

    @Override
    public @NotNull BasicConfig.State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
//        myState = state;
        XmlSerializerUtil.copyBean(state, myState);
    }

    public static BasicConfig getInstance() {
        return ApplicationManager.getApplication().getService(BasicConfig.class);
    }


}
