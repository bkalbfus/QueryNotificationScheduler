package us.akcourts.querynotification;

import java.io.IOException;
import java.util.logging.Logger;

public class Job implements Runnable {
	final static Logger logger = Logger.getLogger(Job.class.getName());
	public String name;
	public String triggerSQL;
	public String command;
	
	public Job(){
		super();
	}
	
	public Job(String name, String triggerSQL, String command) {
		super();
		this.name = name;
		this.triggerSQL = triggerSQL;
		this.command = command;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj.getClass() == Job.class) {
			return ((Job)obj).name.equalsIgnoreCase(this.name);
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public String toString() {
		return "Job {name: " + this.name + ", triggerSQL: " + this.triggerSQL + "command: " + this.command + "}";
	}

	@Override
	// we add the "synchronized" modifier so that only one instance of the command will run at a time when there may be multiple threads tasked with executing it.  For instance, when the job is still executing and the notification condition triggers another message
	public synchronized void run() {
		Process p = null;
		try {
			p = Runtime.getRuntime().exec(this.command);
		} catch (IOException e1) {
			logger.info("Error creating process to execute command: " + this.command);
			e1.printStackTrace();
			return;
		}
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			logger.info("Thread interrupted.");
			e.printStackTrace();
		}
		if(p.exitValue()==0) {
			logger.info("Command Completed normally");
		} else {
			logger.info("Command did not complete normally.  Return value: " + p.exitValue());
		}
		
	}
	
	

}
