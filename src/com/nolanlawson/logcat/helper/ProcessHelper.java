package com.nolanlawson.logcat.helper;

import java.util.EnumMap;
import java.util.List;

public class ProcessHelper {

	public static enum ProcessType {
		Main, Recording;
	}
	
	private static int processesCreated = 0;
	private static int processesKilled = 0;
	
	private static EnumMap<ProcessType, List<Process>> processMap = 
		new EnumMap<ProcessType, List<Process>>(ProcessType.class);
	
	public static void incrementProcesses() {
		processesCreated++;
	}
	
	public static void decrementProcesses() {
		processesKilled++;
	}

	public static int getProcessesCreated() {
		return processesCreated;
	}

	public static int getProcessesKilled() {
		return processesKilled;
	}
	
	public static void registerProcesses(List<Process> processes, ProcessType processType) {
		List<Process> previousProcesses = processMap.get(processType);
		if (previousProcesses != null) {
			// clean up any old processes, just in case we didn't clean them up before
			for (Process process : previousProcesses) {
				process.destroy();
				decrementProcesses();
			}
		}
		processMap.put(processType, processes);
	}
	
	public static void killAll() {
		for (List<Process> processes : processMap.values()) {
			if (processes != null) {
				for (Process process : processes) {
					process.destroy();
					decrementProcesses();
				}
			}
		}
	}
}
