package it.bz.noi.a22scheduler;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import it.bz.noi.a22elaborations.MainElaborations;

public class A22Scheduler extends HttpServlet
{

	private static Logger log = Logger.getLogger(A22Scheduler.class);

	Scheduler scheduler = null;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		try
		{
			// d@vide.bz: don't required by jvm, but required when java code is executed as a war
			Class.forName("org.postgresql.Driver");
			startScheduler();
		}
		catch (Exception e)
		{
			log.error(e);
			throw new ServletException(e);
		}
	}

	@Override
	public void destroy()
	{
		super.destroy();
		try
		{
			scheduler.shutdown();
		}
		catch (SchedulerException e)
		{
			e.printStackTrace();
		}
	}


	private void startScheduler() throws SchedulerException, IOException
	{

		log.info("Start A22Scheduler");

		// Grab the Scheduler instance from the Factory
		System.setProperty("org.quartz.threadPool.threadCount","1");
		System.setProperty("org.quartz.jobStore.misfireThreshold", "500");
		scheduler = StdSchedulerFactory.getDefaultScheduler();

		String schedule;

		// read schedule expression from configuration file
		try (InputStream in = A22Scheduler.class.getResourceAsStream("cron.properties"))
		{
			Properties prop = new Properties();
			prop.load(in);
			schedule = prop.getProperty("schedule");
		}

		JobDetail job = newJob(MainElaborations.class).build();

		Trigger trigger = newTrigger().withSchedule(cronSchedule(schedule)).build();

		scheduler.scheduleJob(job, trigger);

		scheduler.start();

	}
}
