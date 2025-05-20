package org.true_peak.querynotification;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.CharBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.commons.daemon.support.DaemonLoader;
import org.apache.commons.daemon.support.DaemonWrapper;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.configurable.Configurable;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

interface Config { String message(); // message to give 
}

@Component(designate=Config.class)
public class Scheduler extends DaemonWrapper {
	final static Logger logger = Logger.getLogger(Scheduler.class.getName());
	private static final Scheduler scheduler = new Scheduler();
//    private static Engine engine = null;
//    private static launcher  EngineLauncher engineLauncherInstance = new EngineLauncher();
	
	final String sOpts = Configuration.getString("Scheduler.serviceOpts"); //$NON-NLS-1$
	final String sQueueName = Configuration.getString("Scheduler.serviceQueueName"); //$NON-NLS-1$
	final String sTimeout = Configuration.getString("Scheduler.notificationTimeOut");
	final String sConnection = Configuration.getString("Scheduler.jdbcConnectString"); //$NON-NLS-1$
	final String username = Configuration.getString("Scheduler.username"); //$NON-NLS-1$
	final String password = Configuration.getString("Scheduler.password"); //$NON-NLS-1$
	final String odbcDSN = Configuration.getString("Scheduler.odbcDSN"); //$NON-NLS-1$

	private static final short SQL_SUCCESS = 0;
	private static final short SQL_SUCCESS_WITH_INFO = 1;
	private static final short SQL_HANDLE_STMT = 3;
/*  #define SQL_SOPT_SS_BASE                            1225
 	#define SQL_SOPT_SS_QUERYNOTIFICATION_TIMEOUT       (SQL_SOPT_SS_BASE+8) // Notification timeout
	#define SQL_SOPT_SS_QUERYNOTIFICATION_MSGTEXT       (SQL_SOPT_SS_BASE+9) // Notification message text
	#define SQL_SOPT_SS_QUERYNOTIFICATION_OPTIONS       (SQL_SOPT_SS_BASE+10)// SQL service broker name
*/
	private static final short SQL_SOPT_SS_BASE = 1225;
 	private static final short SQL_SOPT_SS_QUERYNOTIFICATION_TIMEOUT = (SQL_SOPT_SS_BASE+8); // Notification timeout
	private static final short SQL_SOPT_SS_QUERYNOTIFICATION_MSGTEXT = (SQL_SOPT_SS_BASE+9); // Notification message text
	private static final short SQL_SOPT_SS_QUERYNOTIFICATION_OPTIONS = (SQL_SOPT_SS_BASE+10);// SQL service broker name
	private ArrayList<Job> jobs;

	Config config;

	  @Activate void activate(Map<String,Object> props) {
		     config = Configurable.createConfigurable(Config.class, props);
		     System.out.println("Hi " + config.message());
		  }

		  @Deactivate void deactivate() {
		     System.out.println("Bye " + config.message());
		  }
	
	
    /* test for SQL_SUCCESS (0) or SQL_SUCCESS_WITH_INFO (1) */
//    #define SQL_SUCCEEDED(rc)  (((rc)&(~1))==0)
    private static boolean SQL_SUCCEEDED(int rc) {
    	return ((rc)&(~1))==0;
//    	return rc == SQL_SUCCESS || rc == SQL_SUCCESS_WITH_INFO;
//    			((rc & SQL_SUCCESS) | (rc & SQL_SUCCESS_WITH_INFO)) == 1;
    }

    private static void extract_error(
    		String fn,
    		int handle,
    		short type) {
    	
    	short	 i = 0;
    	IntByReference	 _native = new IntByReference();
    	String	 state = new String(new char[7]);
    	String	 text =  new String(new char[256]);
    	ShortByReference len = new ShortByReference();
    	int	 ret;
    	logger.info("The driver reported the following diagnostics whilst running " + fn); //$NON-NLS-1$
    	do
    	{
    		ret = CLibrary.INSTANCE.SQLGetDiagRec(type, handle, ++i, state, _native, text,
    			text.length(), len);
    		//if (SQL_SUCCEEDED(ret))
    			logger.info(String.format("%s:%s:%s:%s\n", state, i, _native.getValue(), text)); //$NON-NLS-1$
    	} while (ret == SQL_SUCCESS);  	
    	
    	
    	
    }
	
    // This is the standard, stable way of mapping, which supports extensive
    // customization and mapping of Java to native types.

    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary)
//                Native.loadLibrary(("sqlncli11"),
                Native.loadLibrary(("odbc32"), //$NON-NLS-1$
                               CLibrary.class);
/*
 * SQLRETURN  SQL_API SQLAllocHandle(SQLSMALLINT HandleType,
           SQLHANDLE InputHandle, _Out_ SQLHANDLE *OutputHandle);
        
 */
        int SQLAllocHandle (short HandleType, int InputHandle, IntByReference envHandle);
        
/*
 * SQLRETURN  SQL_API SQLSetEnvAttr(SQLHENV EnvironmentHandle,
           SQLINTEGER Attribute, _In_reads_bytes_opt_(StringLength) SQLPOINTER Value,
           SQLINTEGER StringLength);

 */
        short SQLSetEnvAttr (int EnvironmentHandle, 
        	int Attribute, Pointer  sQL_OV_ODBC3,
        	int StringLength);
        
        short SQLConnect
        (
            int             hdbc,
            String szDSN,
            short         cchDSN,
            String szUID,
            short         cchUID,
            String szAuthStr,
            short         cchAuthStr
        );        
/*SQLRETURN SQL_API SQLSetStmtAttrW(
    SQLHSTMT           hstmt,
    SQLINTEGER         fAttribute,
    SQLPOINTER         rgbValue,
    SQLINTEGER         cbValueMax);
*/
        short SQLSetStmtAttr (
        	    int           hStmt1,
        	    int         fAttribute,
        	    String         sMsg,
        	    int         cbValueMax);
/*SQLRETURN SQL_API SQLExecDirectW
(
    SQLHSTMT    hstmt,
    _In_reads_opt_(TextLength) SQLWCHAR* szSqlStr,
    SQLINTEGER  TextLength
);
*/
        short SQLExecDirect
        (
            int    hstmt,
            String szSqlStr,
            int  TextLength
        );

//SQLRETURN  SQL_API SQLFreeHandle(SQLSMALLINT HandleType, SQLHANDLE Handle);
        short SQLFreeHandle(short HandleType, int Handle);
//SQLRETURN  SQL_API SQLDisconnect(SQLHDBC ConnectionHandle);
        short SQLDisconnect(int ConnectionHandle);
/*SQLRETURN SQL_API SQLGetDiagRecW
(
    SQLSMALLINT     fHandleType,
    SQLHANDLE       handle,
    SQLSMALLINT     iRecord,
    _Out_writes_opt_(6) SQLWCHAR* szSqlState,
    SQLINTEGER*     pfNativeError,
    _Out_writes_opt_(cchErrorMsgMax) SQLWCHAR* szErrorMsg,
    SQLSMALLINT     cchErrorMsgMax,
    SQLSMALLINT*    pcchErrorMsg
);
*/
        short SQLGetDiagRec
        (
            short     fHandleType,
            int       handle,
            short     iRecord,
            String szSqlState,
            IntByReference     _native,
            String szErrorMsg,
            int     i,
            ShortByReference    len
        );        
        
        
        //        void printf(String format, Object... args);
    }

	public static void main(String[] args) throws FileNotFoundException {
		try {
			
			boolean rc = DaemonLoader.load(Scheduler.class.getCanonicalName(), new String[]{});
			DaemonLoader.start();
			Scanner sc = new Scanner(System.in);
	        // wait until receive stop command from keyboard
	        System.out.printf("Enter 'stop' to halt: ");
	        String nextLine = sc.nextLine();
	        while(!nextLine.toLowerCase().equals("stop")){
	        	logger.info("input line: " + nextLine);
	        }
	        logger.info("Checking Scheduler daemon: " + DaemonLoader.check(Scheduler.class.getCanonicalName()));
	        if (DaemonLoader.check(Scheduler.class.getCanonicalName())) {
	            logger.info("stopping Scheduler daemon");
	        	DaemonLoader.stop();
	        }
//			scheduler.init(loader.);
//			scheduler.start();
//		} catch (ClassNotFoundException e) {
//			logger.log(Level.SEVERE, "Could not start Scheduler due to classloading error.", e);
//		} catch (SQLException e) {
//			logger.log(Level.SEVERE, "Could not start Scheduler due to database error", e);
//		} catch (DaemonInitException e) {
//			logger.log(Level.SEVERE, "Could not start Scheduler due to service daemon error", e);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not start Scheduler due to general exception", e);
		}
		
	}
	
	public void start() throws FileNotFoundException, ClassNotFoundException, SQLException {

		for ( Job job : jobs) {
			logger.info(job.name);
			// idea 1:  create a thread for each job.  This thread will subscribe to query notification and do a jdbc wait(receieve
			createQueryNotification(job.triggerSQL,job.name);
		}
		
			Runnable r = new Runnable(){
				@Override
				public void run() {
					try {
						while(!Thread.currentThread().isInterrupted()) 
						{
							monitorQueue();
						}
					} catch (ClassNotFoundException e) {
						logger.log(Level.SEVERE, "Could not start Scheduler due to classloading error.", e);
						Thread.currentThread().interrupt();
					} catch (SQLException e) {
						logger.log(Level.SEVERE, "Could not start Scheduler due to database error", e);
						Thread.currentThread().interrupt();
					}
				}
			};
			Thread t = new Thread(r);
			t.start();
		
	}
	
	private void monitorQueue() throws ClassNotFoundException, SQLException {
		Connection conn = null;
		try {
		    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver"); //Or any other driver //$NON-NLS-1$
		}
		catch(ClassNotFoundException x){
		    logger.info( "SQL Server JDBC driver is not available" ); //$NON-NLS-1$
			logger.throwing(this.getClass().getName(), "monitorQueue", x);
		    throw x;
		}
		try {
			conn = DriverManager.getConnection(sConnection, username, password);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "coudn't connect", e); //$NON-NLS-1$
			logger.throwing(this.getClass().getName(), "monitorQueue", e);
			throw e;
		}
		Statement statement =  conn.createStatement();
		String sql = "waitfor(receive cast(message_body as xml).value('(/*[local-name()=''QueryNotification'']/*[local-name()=''Message''])[1]', 'varchar(100)') job_name from " + sQueueName + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		logger.info("sql: " + sql); //$NON-NLS-1$
		boolean rc = statement.execute(sql);
		if(rc) {
			// message sent to queue and received
			// query notification gets released when satisfied.  Create another.
			ResultSet rs = statement.getResultSet();
			while(rs.next()) {
				// find Job object.  See overridden equals() method in Job class:  defined by name being equal
				String jobName = rs.getString("job_name"); //$NON-NLS-1$
				logger.info("jobName: " + jobName); //$NON-NLS-1$
				Job jobTemplate = new Job(jobName,"",""); //$NON-NLS-1$ //$NON-NLS-2$
				int jobIndex = jobs.indexOf(jobTemplate);  // indexOf uses the equals method of each object in the list to find matching object
				Job job = null;
				if(jobIndex >= 0) {
					job = jobs.get(jobIndex);
					logger.info("Found job: " + job.toString()); //$NON-NLS-1$
				} else {
					String sMsg = "ERROR!!!  Did not find job with name: " + jobName + ".  Cannot refresh notification.  Only one scheduler can poll a given queue."; //$NON-NLS-1$ //$NON-NLS-2$
					logger.info(sMsg);
					continue;
				}
			createQueryNotification(job.triggerSQL, job.name);
			logger.info("Running job command: " + job.command); //$NON-NLS-1$
			new Thread(job).start();
			}
		}
		
		
	}

	void createQueryNotification(String sql, String message) {

    	int ret = 0;
    	IntByReference envHandle = new IntByReference(0);
    	IntByReference hStmt1 =  new IntByReference(0);
    	IntByReference conHandle = new IntByReference(0);
    	 
    	
    	short SQL_HANDLE_ENV = 1;
    	int SQL_NULL_HANDLE = 0;
    	int SQL_SUCCESS = 0;
    	int SQL_ATTR_ODBC_VERSION = 200;
    	Pointer SQL_OV_ODBC3 = new Pointer(3l);
    	int SQL_IS_UINTEGER = -5;
    	short SQL_HANDLE_DBC = 2;
    	short SQL_NTS = -3;

    	
        CLibrary.INSTANCE.SQLAllocHandle(SQL_HANDLE_ENV, SQL_NULL_HANDLE, envHandle);
        if (ret != SQL_SUCCESS)
    		logger.info("SQLAllocHandle failed"); //$NON-NLS-1$
        logger.info("envHandle: " + envHandle.getValue()); //$NON-NLS-1$
        
    	ret = CLibrary.INSTANCE.SQLSetEnvAttr(envHandle.getValue(), SQL_ATTR_ODBC_VERSION, SQL_OV_ODBC3, SQL_IS_UINTEGER);
    	if (ret != SQL_SUCCESS)
    		logger.info("SQLSetEnvAttr conn failed"); //$NON-NLS-1$

    	ret = CLibrary.INSTANCE.SQLAllocHandle(SQL_HANDLE_DBC, envHandle.getValue(), conHandle);
    	if (ret != SQL_SUCCESS)
    		logger.info("SQLAllocHandle conn failed"); //$NON-NLS-1$

    	ret = CLibrary.INSTANCE.SQLConnect(conHandle.getValue(), odbcDSN, SQL_NTS, "crystal", SQL_NTS, "infoview123", SQL_NTS); //$NON-NLS-1$ //$NON-NLS-2$
    	
    	if(ret != SQL_SUCCESS && ret != SQL_SUCCESS_WITH_INFO) {
    		logger.info("SQLConnect conn failed.  ret = " + ret); //$NON-NLS-1$
    	}
    	logger.info(String.format("ret = %d\n", ret)); //$NON-NLS-1$
        

    	if (!SQL_SUCCEEDED(ret))
    	{
    		 extract_error("SQLDriverConnect conHandle", conHandle.getValue(), SQL_HANDLE_DBC); //$NON-NLS-1$
    		System.exit(1);
    	}

    	ret = CLibrary.INSTANCE.SQLAllocHandle(SQL_HANDLE_STMT, conHandle.getValue(), hStmt1);
    	if (!SQL_SUCCEEDED(ret))
    	{
    		extract_error("SQLAllocStmt conHandle", conHandle.getValue(), SQL_HANDLE_DBC); //$NON-NLS-1$
    		System.exit(1);
    	}
    	String sTimeout = Configuration.getString("Scheduler.notificationTimeOut"); //$NON-NLS-1$
    	CharBuffer cbTimeout = CharBuffer.allocate(sTimeout.length());
    	cbTimeout.append(sTimeout);
    	ret = CLibrary.INSTANCE.SQLSetStmtAttr(hStmt1.getValue(), SQL_SOPT_SS_QUERYNOTIFICATION_TIMEOUT, sTimeout,
    			SQL_NTS);

    	String sOpts = "service=ContactChangeNotifications;local database=Onbase_Sup"; //$NON-NLS-1$
    	CharBuffer cbOpts = CharBuffer.allocate(sOpts.length());
    	cbOpts.put(sOpts);
    	ret = CLibrary.INSTANCE.SQLSetStmtAttr(hStmt1.getValue(), SQL_SOPT_SS_QUERYNOTIFICATION_OPTIONS, sOpts,
    			SQL_NTS);

    	String sMsg = message;
    	CharBuffer cbMsg = CharBuffer.allocate(sMsg.length());
    	cbMsg.put(sMsg);
    	ret = CLibrary.INSTANCE.SQLSetStmtAttr(hStmt1.getValue(), SQL_SOPT_SS_QUERYNOTIFICATION_MSGTEXT, sMsg,
    			SQL_NTS);
    	
   		/* If you want to leave the query notification timeout set to its default (5 days), omit this line. */
   		ret = CLibrary.INSTANCE.SQLSetStmtAttr( hStmt1.getValue(), SQL_SOPT_SS_QUERYNOTIFICATION_TIMEOUT, sTimeout, SQL_NTS );

   		/* We want to know if the data returned by running this query changes. */
   		/* Not all queries are compatible with query notifications. Refer to */
   		/* the SQL Server documentation for further information: */
   		/* http://technet.microsoft.com/en-us/library/ms181122(v=sql.105).aspx */
//   		String sql = "select foo, bar from dbo.scrap where bar = 'ho'";
//   		ret = CLibrary.INSTANCE.SQLExecDirect(hStmt1.getValue(), "SELECT ContactID, FirstName, LastName, EmailAddress, EmailPromotion " +
//   	   			"FROM Person.Contact " +
//   	   			"WHERE EmailPromotion IS NOT NULL", SQL_NTS);
   		ret = CLibrary.INSTANCE.SQLExecDirect(hStmt1.getValue(), sql, SQL_NTS);

   		logger.info(String.format("ret = %d\n", ret)); //$NON-NLS-1$

   		CLibrary.INSTANCE.SQLFreeHandle(SQL_HANDLE_STMT, hStmt1.getValue());
   		CLibrary.INSTANCE.SQLDisconnect(conHandle.getValue());
   		CLibrary.INSTANCE.SQLFreeHandle(SQL_HANDLE_DBC, conHandle.getValue());
   		CLibrary.INSTANCE.SQLFreeHandle(SQL_HANDLE_ENV, envHandle.getValue());

   		logger.info("Query notification created successfuly"); //$NON-NLS-1$
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(DaemonContext context) throws DaemonInitException, Exception {
		super.init(context);
		XStream xstream = new XStream(new DomDriver());
		xstream.alias("job", Job.class); //$NON-NLS-1$
// initialize jobs.xml:
//		ArrayList<Job> jobs = new ArrayList<Job>();
//		jobs.add(new Job("test1","select foo, bar from dbo.scrap","cmd /C \"echo hello world \""));
//		xstream.toXML(jobs, new FileOutputStream("jobs.xml"));
		jobs = (ArrayList<Job>) xstream.fromXML(new FileReader(Configuration.getString("Scheduler.jobsList"))); //$NON-NLS-1$
		logger.info(xstream.toXML(jobs));
	}

	@Override
	public void stop() throws Exception {
		Thread.currentThread().interrupt();
		super.stop();
	}

	@Override
	public void destroy() {
		super.destroy();
	}
	

}
