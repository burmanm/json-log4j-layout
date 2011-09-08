package org.elasticflume.log4j;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;

public class JSONLayoutTest {
    private static final Logger DEFAULT_LOGGER = Logger.getLogger("org.elasticsearch");
    private JSONLayout jsonLayout;

    @Before
    public void setup() {
        jsonLayout = new JSONLayout();
        jsonLayout.activateOptions();
    }

    @Test
    public void validateMDCKeys(){
        jsonLayout.setMdcKeysToUse("UserID,RequestID,IPAddress");
        String[] mdckeys = jsonLayout.getMdcKeys();
        assertThat(mdckeys.length, is(3));
        assertThat(mdckeys[0],is("UserID"));
        assertThat(mdckeys[1],is("RequestID"));
        assertThat(mdckeys[2],is("IPAddress"));
    }

    @Test
    public void emptyMDCStringShouldResultInEmptyArray(){
        jsonLayout.setMdcKeysToUse("");
        String[] mdckeys = jsonLayout.getMdcKeys();
        assertThat(mdckeys.length, is(0));

        jsonLayout.setMdcKeysToUse(null);
        mdckeys = jsonLayout.getMdcKeys();
        assertThat(mdckeys.length, is(0));
    }

    @Test
    public void validateBasicLogStructure() {
        LoggingEvent event = createDefaultLoggingEvent();
        String logOutput = jsonLayout.format(event);
        validateBasicLogOutput(logOutput, event);
    }

    @Test
    public void validateMDCValueIsLoggedCorrectly() {
    	LoggingEvent event = populateMDCValuesAndCheckCorrectness();
        String logOutput = jsonLayout.format(event);
        validateBasicLogOutput(logOutput, event);
    	validateMDCValues(logOutput);
    }
    
    private LoggingEvent populateMDCValuesAndCheckCorrectness() {
        Map<String, String> mdcMap = createMapAndPopulateMDC();
        Set<String> mdcKeySet = mdcMap.keySet();

        LoggingEvent event = createDefaultLoggingEvent();
        jsonLayout.setMdcKeysToUse(Joiner.on(",").join(mdcKeySet));
        assertThat(jsonLayout.getMdcKeys().length, is(mdcKeySet.size()));
        for (String key : mdcKeySet) {
            assertThat(jsonLayout.getMdcKeys(), hasItemInArray(key));
        }    	
        return event;
    }
    
    @Test
    public void validateMDCValueIsLoggedCorrectlyTopLevel() {
    	LoggingEvent event = populateMDCValuesAndCheckCorrectness();
    	jsonLayout.setCreateMdcField(Boolean.toString(false));
        String logOutput = jsonLayout.format(event);        
        validateBasicLogOutput(logOutput, event);
    	validateMDCValuesTopLevel(logOutput);
    }

    @Test
    public void validateNDCValueIsLoggedCorrectly() {
        populateNDC();
        LoggingEvent event = createDefaultLoggingEvent();
        String logOutput = jsonLayout.format(event);

        validateBasicLogOutput(logOutput, event);
        assertThat(NDC.getDepth(), is(2));
        validateNDCValues(logOutput);
    }

    @Test
    public void validateExceptionIsLoggedCorrectly() {
        LoggingEvent event = createDefaultLoggingEventWithException();
        String logOutput = jsonLayout.format(event);
        validateExceptionInlogOutput(logOutput);
    }

    private void validateBasicLogOutput(String logOutput, LoggingEvent event) {
        validateLevel(logOutput, event);
        validateLogger(logOutput, event);
        validateThreadName(logOutput, event);
        validateMessage(logOutput, event);
        validateNewLine(logOutput, event);
    }

    private void validateNewLine(String logOutput, LoggingEvent event) {
        assertTrue("every line in a log must end with a new line character",logOutput.endsWith("\n"));
    }

    private void validateLevel(String logOutput, LoggingEvent event) {
        if (event.getLevel() != null) {
            String partialOutput = "\"level\":\"" + event.getLevel().toString() + "\"";
            assertThat(logOutput, containsString(partialOutput));
        } else {
            fail("Expected the level value to be set in the logging event");
        }
    }

    private void validateLogger(String logOutput, LoggingEvent event) {
        if (event.getLogger() != null) {
            String partialOutput = "\"logger\":\"" + event.getLoggerName() + "\"";
            assertThat(logOutput, containsString(partialOutput));
        } else {
            fail("Expected the logger to be set in the logging event");
        }
    }

    private void validateThreadName(String logOutput, LoggingEvent event) {
        if (event.getThreadName() != null) {
            String partialOutput = "\"threadName\":\"" + event.getThreadName() + "\"";
            assertThat(logOutput, containsString(partialOutput));
        } else {
            fail("Expected the threadname to be set in the logging event");
        }
    }

    private void validateMessage(String logOutput, LoggingEvent event) {
        if (event.getMessage() != null) {
            String partialOutput = "\"message\":\"" + event.getMessage() + "\"";
            assertThat(logOutput, containsString(partialOutput));
        } else {
            fail("Expected the message to be set in the logging event");
        }
    }
    
    private void validateMDCValuesTopLevel(String logOutput) {
    	System.out.println(logOutput);
    	String partialOutput = "\"UserId\":\"" + "U1" + "\",\"ProjectId\":\"" + "P1";
        assertThat(logOutput, containsString(partialOutput));
    }

    private void validateMDCValues(String logOutput) {
        String partialOutput = "\"MDC\":{\"UserId\":\"" + "U1" + "\",\"ProjectId\":\"" + "P1" + "\"}";
        assertThat(logOutput, containsString(partialOutput));
    }

    private void validateNDCValues(String logOutput) {
        String partialOutput = "\"NDC\":\"NDC1 NDC2\"";
        assertThat(logOutput, containsString(partialOutput));
    }

    private void validateExceptionInlogOutput(String logOutput) {
        List<String> partialOutput = new ArrayList<String>();
        partialOutput.add("\"throwable\":\"java.lang.IllegalArgumentException: Test Exception in event");
        partialOutput.add("org.elasticflume.log4j.JSONLayoutTest.createDefaultLoggingEventWithException(JSONLayoutTest.java:");
        partialOutput.add("at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)");
        partialOutput.add("at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39");
        partialOutput.add("at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)");
        partialOutput.add("at java.lang.reflect.Method.invoke(Method.java:597)");

        for (String output : partialOutput)
        {
            assertThat(logOutput, containsString(output));
        }
    }

    private LoggingEvent createDefaultLoggingEvent() {
        return new LoggingEvent("", DEFAULT_LOGGER, Level.INFO, "Hello World", null);
    }

    private LoggingEvent createDefaultLoggingEventWithException() {
        return new LoggingEvent("", DEFAULT_LOGGER, Level.INFO, "Hello World", new IllegalArgumentException("Test Exception in event"));
    }

    private Map<String, String> createMapAndPopulateMDC() {
        Map<String, String> mdcMap = new LinkedHashMap<String, String>();
        mdcMap.put("UserId", "U1");
        mdcMap.put("ProjectId", "P1");

        for (Map.Entry<String, String> entry : mdcMap.entrySet()) {
            MDC.put(entry.getKey(), entry.getValue());
        }
        return mdcMap;
    }

    private void populateNDC() {
        NDC.push("NDC1");
        NDC.push("NDC2");
    }
}
