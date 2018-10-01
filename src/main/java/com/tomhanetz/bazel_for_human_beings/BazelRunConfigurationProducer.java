package com.tomhanetz.bazel_for_human_beings;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BazelRunConfigurationProducer extends RunConfigurationProducer<BazelRunConfiguration> {


    protected BazelRunConfigurationProducer(BazelConfigurationType configurationType) {
        super(configurationType);
        cache = new HashMap<>();
    }

    private final Logger log = Logger.getInstance("Bazel Producer");

    private Map<String, String> cache;

    private String getBazelPath(ConfigurationContext configurationContext) throws IOException {
        String bazelPath = BazelApplicationSettings.getInstance().getBazelQueryPath();
        String directoryPath = configurationContext.getLocation().getVirtualFile().getParent().getPath();
        String name = configurationContext.getLocation().getVirtualFile().getName();

        if (cache.containsKey(directoryPath+":::"+name)){
            return cache.get(directoryPath+":::"+name);
        }

        String bazelExecutablePath = Utils.runCommand(new String[]{bazelPath, "query",
                "attr('srcs', '" + name + "', ':*')"}, directoryPath);

        cache.put(directoryPath+":::"+name, bazelExecutablePath);

        return bazelExecutablePath;
    }

    @Override
    protected boolean setupConfigurationFromContext(BazelRunConfiguration bazelRunConfiguration, ConfigurationContext configurationContext, Ref<PsiElement> ref) {
        long start = System.currentTimeMillis();
        try {
            // returns //a/b/c:my_exec
            String bazelExecutablePath = getBazelPath(configurationContext);
            bazelRunConfiguration.setName("BAZEL: " + bazelExecutablePath);
            log.info("Successfully received bazel executable path of: " + bazelExecutablePath);
            long end = System.currentTimeMillis();
            log.info(String.format("one command on this shitty bazel took %d millis", (end-start)));

            bazelRunConfiguration.setBazelExecutablePath(bazelExecutablePath);
            return true;
        }catch (Exception exception){
            log.warn("Received an exception: " + exception.getMessage());
            return false;
        }
    }

    @Override
    public boolean isConfigurationFromContext(BazelRunConfiguration bazelRunConfiguration, ConfigurationContext configurationContext) {
        try {
            String bazelPath = getBazelPath(configurationContext);
            String runConfigurationBazelPath = bazelRunConfiguration.getBazelExecutablePath();
            return runConfigurationBazelPath != null && runConfigurationBazelPath.equals(bazelPath);
        }catch (IOException exception){
            log.warn("Received an io exception: " + exception.getMessage());
            return false;
        }
    }
}