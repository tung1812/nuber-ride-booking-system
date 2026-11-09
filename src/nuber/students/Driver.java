package nuber.students;

import java.util.concurrent.ThreadLocalRandom;

public class Driver extends Person {
	
	private Passenger currentPassenger;
	
	public Driver(String driverName, int maxSleep)
	{
		super(driverName, maxSleep);
	}
	
	/**
	 * Stores the provided passenger as the driver's current passenger and then
	 * sleeps the thread for between 0-maxDelay milliseconds.
	 * 
	 * @param newPassenger Passenger to collect
	 * @throws InterruptedException
	 */
	public void pickUpPassenger(Passenger newPassenger)
	{
        this.currentPassenger = newPassenger;
        try {
            int delay = ThreadLocalRandom.current().nextInt(0, this.maxSleep + 1);
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
	}

	/**
	 * Sleeps the thread for the amount of time returned by the current 
	 * passenger's getTravelTime() function
	 * 
	 * @throws InterruptedException
	 */
	public void driveToDestination() {
        if (currentPassenger == null) return;
        try {
            Thread.sleep(currentPassenger.getTravelTime()); // sleep for passenger.getTravleTime()
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            currentPassenger = null; // clear it
        }
	}
	
}
